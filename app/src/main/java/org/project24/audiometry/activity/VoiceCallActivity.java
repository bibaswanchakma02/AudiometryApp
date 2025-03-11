package org.project24.audiometry.activity;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.project24.audiometry.R;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

public class VoiceCallActivity extends AppCompatActivity {

    private RtcEngine rtcEngine;
    private boolean isMuted = false;
    private boolean isSpeakerOn = true;
    private static final String APP_ID = "2f8d30c2ffdb418bb1135c0889d6bc37";
    private static final String CHANNEL_NAME = "test_channel";
    private static final String TOKEN = "YOUR_AGORA_TOKEN";

    private ImageButton btnMute, btnSpeaker, btnEndCall;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);

        btnMute = findViewById(R.id.btnMic);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnEndCall = findViewById(R.id.btnEndCall);

        initializeAgoraEngine();

        btnMute.setOnClickListener(v -> toggleMute());
        btnSpeaker.setOnClickListener(v -> toggleSpeaker());
        btnEndCall.setOnClickListener(v -> endCall());
    }

    private void initializeAgoraEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = APP_ID;
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    runOnUiThread(() -> Toast.makeText(VoiceCallActivity.this, "Joined Channel: " + channel, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    runOnUiThread(() -> {
                        Toast.makeText(VoiceCallActivity.this, "User Offline", Toast.LENGTH_SHORT).show();
                        endCall();
                    });
                }
            };

            rtcEngine = RtcEngine.create(config);
            rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            rtcEngine.disableVideo(); // Voice-only call
            rtcEngine.enableAudio();
            rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
            rtcEngine.setEnableSpeakerphone(true);

            rtcEngine.joinChannel(TOKEN, CHANNEL_NAME, null, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleMute() {
        isMuted = !isMuted;
        rtcEngine.muteLocalAudioStream(isMuted);
        btnMute.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        rtcEngine.setEnableSpeakerphone(isSpeakerOn);
        btnSpeaker.setImageResource(isSpeakerOn ? R.drawable.ic_speaker_on : R.drawable.ic_speaker);
    }

    private void endCall() {
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }
}
