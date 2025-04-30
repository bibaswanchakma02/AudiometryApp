package org.project24.audiometry.activity;


import static io.agora.rtc2.Constants.USER_OFFLINE_QUIT;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.project24.audiometry.R;

import io.agora.rtc2.*;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class VideoCallActivity extends AppCompatActivity {

    public static final String AGORA_APP_ID = "2f8d30c2ffdb418bb1135c0889d6bc37"; // Replace with your Agora App ID
    public static final String EXTRA_CHANNEL_NAME = "channel_name"; // Replace with your channel name
    private static final int UID = 0;
    private static final int PERMISSION_REQ_ID = 22;

    private RtcEngine mRtcEngine;
    private FrameLayout localContainer, remoteContainer;
    private boolean isJoined = false;
    private String channelName;
    private boolean isInitiator = false;
    private String recipientId = null;
    private DatabaseReference callRef = null;
    private static final String TAG = "VideoCallActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        // Retrieve channel name from intent
        channelName = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);
        Log.d("VideoCallActivity", "Received Channel Name: " + channelName);
        isInitiator = getIntent().getBooleanExtra("isInitiator", false);
        recipientId = getIntent().getStringExtra("recipientId");

        if (channelName == null || channelName.isEmpty()) {
            Log.e("VideoCallActivity", "Invalid channel_name received");
            Toast.makeText(this, "Invalid channel_name", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        localContainer = findViewById(R.id.local_video_view_container);
        remoteContainer = findViewById(R.id.remote_video_view_container);


        ImageButton endCallButton = findViewById(R.id.btn_end_call);
        endCallButton.setOnClickListener(v -> leaveChannel());

        // Check and request permissions
        if (checkPermissions()) {
            initializeAgora();
        } else {
            requestPermissions();
        }

        if (!isInitiator) {
            updateCallStatus("answered");
        }

        // Listen for remote user leaving
        setupCallStatusListener();
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        }, PERMISSION_REQ_ID);
    }

    private void setupCallStatusListener() {
        // Find the call in Firebase based on channel name
        DatabaseReference callsRef = FirebaseDatabase.getInstance().getReference("Calls");
        callsRef.orderByChild("channelName").equalTo(channelName).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot callSnapshot : snapshot.getChildren()) {
                                callRef = callSnapshot.getRef();

                                // Update call status if we're answering
                                if (!isInitiator) {
                                    callRef.child("status").setValue("answered");
                                }

                                // Listen for status changes (e.g., if the other party ends the call)
                                callRef.child("status").addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String status = snapshot.getValue(String.class);
                                        if ("ended".equals(status)) {
                                            // The other party ended the call
                                            if (isJoined) {
                                                Toast.makeText(VideoCallActivity.this,
                                                        "Call ended by the other party",
                                                        Toast.LENGTH_SHORT).show();
                                                leaveChannel();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Call status listener cancelled");
                                    }
                                });

                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to find call reference");
                    }
                });
    }

    private void updateCallStatus(String status) {
        if (callRef != null) {
            callRef.child("status").setValue(status);
        } else {
            // If we don't have the callRef yet, search for it
            DatabaseReference callsRef = FirebaseDatabase.getInstance().getReference("Calls");
            callsRef.orderByChild("channelName").equalTo(channelName).limitToFirst(1)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot callSnapshot : snapshot.getChildren()) {
                                    callSnapshot.getRef().child("status").setValue(status);
                                    break;
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to update call status");
                        }
                    });
        }
    }

    private void initializeAndJoinChannel() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = AGORA_APP_ID;
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    Log.d(TAG, "Successfully joined channel: " + channel);
                    isJoined = true;
                    runOnUiThread(() -> {
                        Toast.makeText(VideoCallActivity.this,
                                "Successfully joined call",
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    Log.d(TAG, "Remote user joined: " + uid);
                    runOnUiThread(() -> {
                        Toast.makeText(VideoCallActivity.this,
                                "Remote user joined the call",
                                Toast.LENGTH_SHORT).show();
                        setupRemoteVideo(uid);
                    });
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    Log.d(TAG, "Remote user left: " + uid);
                    runOnUiThread(() -> {
                        Toast.makeText(VideoCallActivity.this,
                                "Remote user left the call",
                                Toast.LENGTH_SHORT).show();
                        removeRemoteVideo();

                        // Consider ending the call if remote user left
                        if (reason == USER_OFFLINE_QUIT) {
                            leaveChannel();
                        }
                    });
                }

                @Override
                public void onError(int err) {
                    Log.e(TAG, "Agora error: " + err);
                    runOnUiThread(() -> {
                        Toast.makeText(VideoCallActivity.this,
                                "Error code: " + err,
                                Toast.LENGTH_SHORT).show();

//                        if (err == ERROR_START_CAMERA || err == ERROR_START_VIDEO_CAPTURE) {
//                            Toast.makeText(VideoCallActivity.this,
//                                    "Failed to start camera",
//                                    Toast.LENGTH_LONG).show();
//                        }
                    });
                }
            };

            mRtcEngine = RtcEngine.create(config);

            // Configure the video encoding settings
            mRtcEngine.enableVideo();

            // Optional: Set video encoder configuration
            VideoEncoderConfiguration configuration = new VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,  // Resolution
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15, // Frame rate
                    VideoEncoderConfiguration.STANDARD_BITRATE, // Bitrate
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE // Orientation
            );
            mRtcEngine.setVideoEncoderConfiguration(configuration);

            setupLocalVideo();
            joinChannel();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing RTC engine", e);
            Toast.makeText(this, "Error initializing video call: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeAgora() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = AGORA_APP_ID;
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onUserJoined(int uid, int elapsed) {
                    runOnUiThread(() -> setupRemoteVideo(uid));
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    runOnUiThread(() -> removeRemoteVideo());
                }
            };

            mRtcEngine = RtcEngine.create(config);
            mRtcEngine.enableVideo();
            setupLocalVideo();
            joinChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupLocalVideo() {
        SurfaceView localView = new SurfaceView(this);
        localContainer.addView(localView);

        VideoCanvas localVideoCanvas = new VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, UID);
        mRtcEngine.setupLocalVideo(localVideoCanvas);
        mRtcEngine.startPreview();
    }

    private void setupRemoteVideo(int uid) {
        SurfaceView remoteView = new SurfaceView(this);
        remoteContainer.addView(remoteView);

        VideoCanvas remoteVideoCanvas = new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid);
        mRtcEngine.setupRemoteVideo(remoteVideoCanvas);
    }

    private void removeRemoteVideo() {
        remoteContainer.removeAllViews();
    }

    private void joinChannel() {
        String token = null; // Use a token if your Agora app requires one
//        String channel = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);

        mRtcEngine.joinChannel(token, channelName, null, UID);
        isJoined = true;
    }

    private void leaveChannel() {
        if (mRtcEngine != null && isJoined) {
            mRtcEngine.leaveChannel();
            isJoined = false;
        }
        finish(); // Close activity when call ends
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRtcEngine != null) {
            RtcEngine.destroy();
            mRtcEngine = null;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeAgora();
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}