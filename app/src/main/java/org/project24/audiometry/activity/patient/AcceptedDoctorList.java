package org.project24.audiometry.activity.patient;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.project24.audiometry.R;
import org.project24.audiometry.activity.ChatActivity;

public class AcceptedDoctorList extends AppCompatActivity {

    private TextView appointmentStatus, doctorName;
    private Button startTestButton, chatButton;
    private DatabaseReference appointmentRef;
    private String patientId, doctorId, chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accepted_doctor_list);

        appointmentStatus = findViewById(R.id.appointmentStatus);
        doctorName = findViewById(R.id.doctorName);
        startTestButton = findViewById(R.id.startTestButton);
        chatButton = findViewById(R.id.chatButton);

        patientId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments");

        loadAppointmentDetails();

        /*startTestButton.setOnClickListener(v -> {
            Intent intent = new Intent(AcceptedDoctorList.this, PerformTest.class);
            startActivity(intent);
        });

         */

        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(AcceptedDoctorList.this, ChatActivity.class);
            intent.putExtra("doctorId", doctorId);
            intent.putExtra("patientId", patientId);
            startActivity(intent);
        });
    }

    private void loadAppointmentDetails() {
        appointmentRef.orderByChild("patientId").equalTo(patientId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                String status = dataSnapshot.child("status").getValue(String.class);
                                doctorId = dataSnapshot.child("doctorId").getValue(String.class);
                                String doctor = dataSnapshot.child("doctorName").getValue(String.class);

                                appointmentStatus.setText("Status: " + status);
                                doctorName.setText("Doctor: " + doctor);

                                if ("Accepted".equals(status)) {
                                    startTestButton.setVisibility(View.VISIBLE);
                                    chatButton.setVisibility(View.VISIBLE);
                                } else {
                                    startTestButton.setVisibility(View.GONE);
                                    chatButton.setVisibility(View.GONE);
                                }
                            }
                        } else {
                            Log.e("FirebaseError", "No appointment found for patient ID: " + patientId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", "Failed to read data: " + error.getMessage());
                    }
                });
    }


}
