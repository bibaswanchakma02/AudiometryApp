package org.project24.audiometry.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.project24.audiometry.R;

import io.agora.rtc2.*;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class VideoCallActivity extends AppCompatActivity {

    private static final String AGORA_APP_ID = "2f8d30c2ffdb418bb1135c0889d6bc37"; // Replace with your Agora App ID
    private static final String CHANNEL_NAME = "testChannel"; // Replace with your channel name
    private static final int UID = 0;
    private static final int PERMISSION_REQ_ID = 22;

    private RtcEngine mRtcEngine;
    private FrameLayout localContainer, remoteContainer;
    private boolean isJoined = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

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
        mRtcEngine.joinChannel(token, CHANNEL_NAME, null, UID);
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