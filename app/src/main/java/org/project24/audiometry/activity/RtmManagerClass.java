package org.project24.audiometry.activity;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMediaOperationProgress;
import io.agora.rtm.RtmMessage;

public class RtmManagerClass {
    private static final String AGORA_APP_ID = "2f8d30c2ffdb418bb1135c0889d6bc37";
    private static RtmClient rtmClient;
    private static RtmClientListener rtmListener;
    private Context appContext;

    public RtmManagerClass(Context context) {
        this.appContext = context.getApplicationContext();
        initRtm();
    }

    private void initRtm() {
        try {
            Log.d("RealTime", "Initializing RTM client...");
            rtmListener = new RtmClientListener() {
                @Override
                public void onConnectionStateChanged(int i, int i1) {

                }

                @Override
                public void onMessageReceived(RtmMessage rtmMessage, String s) {
                    Log.d("RealTime", "Message received: " + rtmMessage.getText());
                    try {


                        JSONObject data = new JSONObject(rtmMessage.getText());
                        String type = data.getString("type");

                        if ("video_call".equals(type)) {
                            String channelName = data.getString("channel_name");
                            String callerId = data.getString("caller_id");
                            Log.d("RealTime", "Received video_call type with channelName: " + channelName);
//                            Intent intent = new Intent(appContext, IncomingCallActivity.class);
//                            intent.putExtra("channelName", channelName);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            appContext.startActivity(intent);
                            if (channelName != null && !channelName.isEmpty()) {
                                Intent intent = new Intent(appContext, IncomingCallActivity.class);

                                // FIXED: Use the correct constant for channel name
                                intent.putExtra(VideoCallActivity.EXTRA_CHANNEL_NAME, channelName);
                                intent.putExtra(IncomingCallActivity.EXTRA_CALLER_ID, callerId);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Log.d("RealTime", "Starting IncomingCallActivity with channelName: " + channelName);
                                appContext.startActivity(intent);
                            } else {
                                Log.e("RealTime", "Received empty channelName");
                            }

                        }else {
                            Log.d("RealTime", "Received message with unsupported type: " + type);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onImageMessageReceivedFromPeer(RtmImageMessage rtmImageMessage, String s) {

                }

                @Override
                public void onFileMessageReceivedFromPeer(RtmFileMessage rtmFileMessage, String s) {

                }

                @Override
                public void onMediaUploadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {

                }

                @Override
                public void onMediaDownloadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {

                }

                @Override
                public void onTokenExpired() {

                }

                @Override
                public void onPeersOnlineStatusChanged(Map<String, Integer> map) {
                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        Log.d("RealTime", "Peer online status changed: " + entry.getKey() + " is " +
                                (entry.getValue() == 0 ? "online" : "offline"));
                    }
                }

                // other overrides...
            };

            rtmClient = RtmClient.createInstance(appContext, AGORA_APP_ID, rtmListener);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RtmClient getClient() {
        if (rtmClient == null) {
            Log.d("RealTime", "RTM client is null, trying to reinitialize");
            initRtm();
        }
        return rtmClient;
    }
}
