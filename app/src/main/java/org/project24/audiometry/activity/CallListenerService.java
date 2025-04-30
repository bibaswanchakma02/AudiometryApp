package org.project24.audiometry.activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.project24.audiometry.activity.patient.MainActivity;

import java.security.Provider;

public class CallListenerService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static final String TAG = "CallListenerService";
    private static final String CHANNEL_ID = "incoming_calls_channel";
    private static final int NOTIFICATION_ID = 1001;

    private DatabaseReference callsRef;
    private ChildEventListener callsListener;
    private String currentUserId;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel for foreground service
        createNotificationChannel();

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createServiceNotification());

        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        callsRef = FirebaseDatabase.getInstance().getReference("Calls");

        // Set up listener for new calls
        setupCallsListener();

        Log.d(TAG, "Call listener service started for user: " + currentUserId);
    }

    private void setupCallsListener() {
        callsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                try {
                    // Check if this call is for the current user
                    String recipientId = snapshot.child("recipientId").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);

                    if (currentUserId.equals(recipientId) && "pending".equals(status)) {
                        String callerId = snapshot.child("callerId").getValue(String.class);
                        String channelName = snapshot.child("channelName").getValue(String.class);
                        String type = snapshot.child("type").getValue(String.class);

                        Log.d(TAG, "Incoming call detected: " + channelName + " from " + callerId);

                        // Show incoming call screen
                        showIncomingCallScreen(callerId, channelName, type);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing call notification", e);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // We could handle call status changes here
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle removed calls if needed
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Not needed for this implementation
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Call listener cancelled");
            }
        };

        callsRef.addChildEventListener(callsListener);

        // Clean up expired calls
//        cleanupExpiredCalls();
    }

    private Notification createServiceNotification() {
        // Create an intent for the user to tap on to open the main activity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentTitle("Call Service Active")
                .setContentText("Waiting for incoming calls")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent);

        return builder.build();
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Incoming Calls";
            String description = "Notifications for incoming audio and video calls";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Log.d(TAG, "Notification channel created");
        }
    }

    private void showIncomingCallScreen(String callerId, String channelName, String type) {
        // Get caller's name from Users database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(callerId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String callerName = snapshot.child("fullname").getValue(String.class);
                if (callerName == null) callerName = "Unknown Caller";

                // Start IncomingCallActivity
                Intent intent = new Intent(CallListenerService.this, IncomingCallActivity.class);
                intent.putExtra(VideoCallActivity.EXTRA_CHANNEL_NAME, channelName);
                intent.putExtra(IncomingCallActivity.EXTRA_CALLER_ID, callerId);
                intent.putExtra("callerName", callerName);
                intent.putExtra("callType", type);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to get caller info", error.toException());

                // Start IncomingCallActivity anyway
                Intent intent = new Intent(CallListenerService.this, IncomingCallActivity.class);
                intent.putExtra(VideoCallActivity.EXTRA_CHANNEL_NAME, channelName);
                intent.putExtra(IncomingCallActivity.EXTRA_CALLER_ID, callerId);
                intent.putExtra("callerName", "Unknown Caller");
                intent.putExtra("callType", type);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // This ensures the service restarts if it's killed by the system
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Remove the call listener when service is destroyed
        if (callsListener != null && callsRef != null) {
            callsRef.removeEventListener(callsListener);
            Log.d(TAG, "Call listener removed");
        }

        super.onDestroy();
    }
}
