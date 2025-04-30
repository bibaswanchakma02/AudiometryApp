package org.project24.audiometry.activity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.project24.audiometry.R;



public class IncomingCallActivity extends AppCompatActivity {
    public static final String EXTRA_CALLER_ID = "caller_id";
//    public static final String EXTRA_CHANNEL_NAME = "channel_name";

    private static final String TAG = "IncomingCallActivity";

    private MediaPlayer ringtonePlayer;
    private String callerId;
    private String channelName;
    private DatabaseReference callRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        callerId = getIntent().getStringExtra(EXTRA_CALLER_ID);
        // Retrieve the channel name from the incoming intent
        channelName = getIntent().getStringExtra(VideoCallActivity.EXTRA_CHANNEL_NAME);
        Log.d("IncomingCallActivity", "Intent extras: " + getIntent().getExtras());
        Log.d("IncomingCallActivity", "Caller ID: " + callerId);
        Log.d("IncomingCallActivity", "Received Channel Name: " + channelName);

        // If channel name is null, create a default one based on caller ID
        if (channelName == null || channelName.isEmpty()) {
            // Generate a channel name if none was provided
            channelName = "call_" + callerId + "_" + System.currentTimeMillis();
            Log.d("IncomingCallActivity", "Generated new channel name: " + channelName);
        }

        TextView callerText = findViewById(R.id.text_caller_id);
        callerText.setText("Incoming call from: " + callerId);

        Button acceptButton = findViewById(R.id.btn_accept);
        Button declineButton = findViewById(R.id.btn_decline);

        Log.d("IncomingCallActivity", "Channel Name: " + channelName);


        acceptButton.setOnClickListener(v -> acceptCall());
        declineButton.setOnClickListener(v -> declineCall());

//        playRingtone();
    }

    private void findCallReference() {
        // Find the call in Firebase based on channel name
        DatabaseReference callsRef = FirebaseDatabase.getInstance().getReference("Calls");
        Query query = callsRef.orderByChild("channelName").equalTo(channelName).limitToFirst(1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot callSnapshot : snapshot.getChildren()) {
                        callRef = callSnapshot.getRef();

                        // Listen for status changes (e.g., if caller hangs up before we answer)
                        callRef.child("status").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String status = snapshot.getValue(String.class);
                                if ("cancelled".equals(status) || "ended".equals(status)) {
                                    // Caller cancelled the call before we answered
                                    Toast.makeText(IncomingCallActivity.this,
                                            "Call has ended", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Call status listener cancelled", error.toException());
                            }
                        });

                        break;
                    }
                } else {
                    Log.e(TAG, "No call found with channel: " + channelName);
                    Toast.makeText(IncomingCallActivity.this,
                            "Call information not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to find call reference", error.toException());
            }
        });
    }

    private void updateCallStatus(String status) {
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
                        Log.e(TAG, "Failed to update call status", error.toException());
                    }
                });
    }

    private void playRingtone() {
        ringtonePlayer = MediaPlayer.create(this, R.raw.incoming_ringtone);
        ringtonePlayer.setLooping(true);
        ringtonePlayer.start();
    }

    private void stopRingtone() {
        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
    }

    private void acceptCall() {
        stopRingtone();
        Log.d("IncomingCallActivity", "Channel Name to pass: " + channelName);
        Intent intent = new Intent(this, VideoCallActivity.class);
        intent.putExtra(VideoCallActivity.EXTRA_CHANNEL_NAME, channelName);
        startActivity(intent);
        finish();
    }


    private void declineCall() {
//        stopRingtone();
        // Optionally send an RTM message back to the caller: "Call declined"
        finish();
    }

    @Override
    protected void onDestroy() {
        stopRingtone();
        super.onDestroy();
    }
}
