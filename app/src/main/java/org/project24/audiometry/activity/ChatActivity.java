package org.project24.audiometry.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.project24.audiometry.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton, phoneCallButton, videoCallButton;
    private TextView chatHeader;
    private List<Message> messageList;
    private ChatAdapter chatAdapter;
    private DatabaseReference chatRef, userRef;
    private String doctorId, patientId, chatId, currentUserId, recipientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRecyclerView = findViewById(R.id.chatListView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        phoneCallButton = findViewById(R.id.phoneCallButton);
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

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList, currentUserId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        fetchRecipientFullName(); // Fetch and display full name
        loadMessages();

        sendButton.setOnClickListener(v -> sendMessage());
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
