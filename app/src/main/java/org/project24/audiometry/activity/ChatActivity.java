package org.project24.audiometry.activity;



import android.content.Intent;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;


import org.json.JSONObject;
import org.project24.audiometry.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.SendMessageOptions;


public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton, voiceCallButton, videoCallButton;
    private TextView chatHeader;
    private List<Message> messageList;
    private ChatAdapter chatAdapter;
    private DatabaseReference chatRef, userRef;
    private String doctorId, patientId, chatId, currentUserId, recipientId;
    public RtmManagerClass rtmManagerClass;
    private RtmClient rtmClient;
    private boolean isRtmInitialized = false;
    private static final String TAG = "ChatActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRecyclerView = findViewById(R.id.chatListView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        voiceCallButton = findViewById(R.id.phoneCallButton);
        videoCallButton = findViewById(R.id.videoCallButton);
        chatHeader = findViewById(R.id.chatHeader);

        // Get doctorId and patientId from Intent
        doctorId = getIntent().getStringExtra("doctorId");
        patientId = getIntent().getStringExtra("patientId");

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatId = doctorId + "_" + patientId;  // Unique chat ID
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(chatId).child("messages");

        // Determine recipient (the other user in the chat)
        recipientId = currentUserId.equals(doctorId) ? patientId : doctorId;

//        initializeRtm();

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList, currentUserId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);


        fetchRecipientFullName(); // Fetch and display full name
        loadMessages();

        sendButton.setOnClickListener(v -> sendMessage());
//        videoCallButton.setOnClickListener(v -> {
//            Intent intent = new Intent(ChatActivity.this, VideoCallActivity.class);
//            startActivity(intent);
//        });
        videoCallButton.setOnClickListener(v -> initiateVideoCall());
        voiceCallButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, VoiceCallActivity.class);
            startActivity(intent);
        });
    }


//    private void initializeRtm(){
//        try{
//            rtmManagerClass = new RtmManagerClass(this);
//            rtmClient = rtmManagerClass.getClient();
//
//            if(rtmClient!= null && currentUserId != null){
//                rtmClient.login(null, currentUserId, new ResultCallback<Void>() {
//                    @Override
//                    public void onSuccess(Void unused) {
//                        Log.d("RealTime", "RTM client login successful");
//                        isRtmInitialized = true;
//                    }
//
//                    @Override
//                    public void onFailure(ErrorInfo errorInfo) {
//                        Toast.makeText(ChatActivity.this,
//                                "Failed to initialize call service: " + errorInfo.getErrorDescription(),
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }else{
//                Log.e("RealTime", "RTM client or currentUserId is null");
//            }
//        }catch (Exception e){
//            Log.e("RealTime", "Error initializing RTM", e);
//        }
//
//
//    }

    private void initiateVideoCall(){
//        if (!isRtmInitialized) {
//            Toast.makeText(this, "Call service not initialized. Please try again.", Toast.LENGTH_SHORT).show();
//            // Try to initialize RTM again
//            initializeRtm();
//            return;
//        }
//        if (recipientId == null || recipientId.isEmpty()) {
//            Toast.makeText(this, "Recipient ID not available", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        String channelName = "call_" + currentUserId + "_" + recipientId + "_" + System.currentTimeMillis();
//        try {
//            // Create call invitation message
//            JSONObject messageData = new JSONObject();
//            messageData.put("type", "video_call");
//            messageData.put("channel_name", channelName);
//            messageData.put("caller_id", currentUserId);
//
//            Log.d("RealTime", "Sending video call invitation to " + recipientId + " with channel: " + channelName);
//
//            // Create and send RTM message
//            RtmMessage message = rtmClient.createMessage(messageData.toString());
//            SendMessageOptions options = new SendMessageOptions();
//
//            rtmClient.sendMessageToPeer(recipientId, message, options, new ResultCallback<Void>() {
//                @Override
//                public void onSuccess(Void unused) {
//                    Log.d("RealTime", "Video call invitation sent successfully");
//
//                    // Start VideoCallActivity
//                    runOnUiThread(() -> {
//                        Intent intent = new Intent(ChatActivity.this, VideoCallActivity.class);
//                        intent.putExtra(VideoCallActivity.EXTRA_CHANNEL_NAME, channelName);
//                        startActivity(intent);
//                    });
//                }
//
//                @Override
//                public void onFailure(ErrorInfo errorInfo) {
//                    Log.e("RealTime", "Failed to send video call invitation: " + errorInfo.getErrorDescription());
//                    runOnUiThread(() -> {
//                        Toast.makeText(ChatActivity.this,
//                                "Failed to send invitation: " + errorInfo.getErrorDescription(),
//                                Toast.LENGTH_SHORT).show();
//                    });
//                }
//            });
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        Log.d(TAG, "Initiating video call with recipient: " + recipientId);

        if (recipientId == null || recipientId.isEmpty()) {
            Log.e(TAG, "Cannot initiate call: Recipient ID not available");
            Toast.makeText(this, "Cannot initiate call: Recipient not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a unique channel name that both parties can derive
        // This approach ensures that both caller and callee use the same channel
        String channelName = generateChannelName(currentUserId, recipientId);
        Log.d(TAG, "Generated channel name: " + channelName);

        // Start VideoCallActivity directly
        Intent intent = new Intent(ChatActivity.this, VideoCallActivity.class);
        intent.putExtra(VideoCallActivity.EXTRA_CHANNEL_NAME, channelName);
        intent.putExtra("isInitiator", true); // Add flag to indicate this user started the call
        intent.putExtra("recipientId", recipientId); // Pass recipient ID for potential Firebase notification

        // Store call information in Firebase to notify recipient
        sendCallNotification(channelName, recipientId);

        startActivity(intent);
    }

    /**
     * Generates a consistent channel name between two users
     * Ensures same channel regardless of who initiates
     */

    private String generateChannelName(String userId1, String userId2) {
        // Sort the IDs to ensure the same channel name regardless of who initiates
        String[] ids = {userId1, userId2};
        java.util.Arrays.sort(ids);

        // Create a channel name with timestamp to make it unique for each call session
        return "call_" + ids[0] + "_" + ids[1] + "_" + System.currentTimeMillis();
    }

    private void sendCallNotification(String channelName, String recipientUserId) {
        try {
            // Reference to the calls collection in Firebase
            DatabaseReference callsRef = FirebaseDatabase.getInstance().getReference("Calls");

            // Create a new call entry
            String callId = callsRef.push().getKey();
            if (callId == null) {
                Log.e(TAG, "Failed to create call notification: null call ID");
                return;
            }

            // Create call data
            DatabaseReference callRef = callsRef.child(callId);
            callRef.child("channelName").setValue(channelName);
            callRef.child("callerId").setValue(currentUserId);
            callRef.child("recipientId").setValue(recipientUserId);
            callRef.child("timestamp").setValue(ServerValue.TIMESTAMP);
            callRef.child("type").setValue("video"); // or "voice" for voice calls
            callRef.child("status").setValue("pending");

            // Set a TTL (Time To Live) for this call notification - auto-delete after 60 seconds
            // if not answered
            callRef.child("expiresAt").setValue(ServerValue.TIMESTAMP);

            Log.d(TAG, "Call notification sent to Firebase for recipient: " + recipientUserId);

            // You could also send an FCM notification here if you want to wake up the recipient's device

        } catch (Exception e) {
            Log.e(TAG, "Error sending call notification", e);
        }
    }

    private void fetchRecipientFullName() {
        if (recipientId == null) {
            chatHeader.setText("User Not Found");
            Log.e("ChatActivity", "Error: recipientId is NULL");
            return;
        }

        Log.d("ChatActivity", "Fetching full name for recipientId: " + recipientId);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(recipientId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullname").getValue(String.class); // Ensure it's 'fullname', not 'fullName'

                    if (fullName != null) {
                        chatHeader.setText(fullName);
                        Log.d("ChatActivity", "Full Name Fetched: " + fullName);
                    } else {
                        chatHeader.setText("Name Not Available");
                        Log.e("ChatActivity", "Full Name is NULL for user: " + recipientId);
                    }
                } else {
                    chatHeader.setText("User Not Found");
                    Log.e("ChatActivity", "User data does not exist for: " + recipientId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatActivity", "Database Error: " + error.getMessage());
            }
        });
    }


    private void loadMessages() {
        chatRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Message message = data.getValue(Message.class);
                    if (message != null) {
                        message.setFormattedTimestamp(formatTimestamp(message.getTimestamp()));
                        messageList.add(message);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String messageId = chatRef.push().getKey();
        Message message = new Message(currentUserId, text, System.currentTimeMillis());

        chatRef.child(messageId).setValue(message);
        messageInput.setText("");
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }



}
