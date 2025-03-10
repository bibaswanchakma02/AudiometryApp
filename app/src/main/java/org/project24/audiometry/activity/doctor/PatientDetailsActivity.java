package org.project24.audiometry.activity.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.project24.audiometry.activity.ChatActivity;
import org.project24.audiometry.R;

public class PatientDetailsActivity extends AppCompatActivity {

    private TextView patientNameText, appointmentDateText, appointmentTimeText;
    private Button startTestButton, previousReportButton, startChatButton;
    private String patientId, appointmentId, doctorId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        patientNameText = findViewById(R.id.patientNameText);
        appointmentDateText = findViewById(R.id.appointmentDateText);
        appointmentTimeText = findViewById(R.id.appointmentTimeText);
        startTestButton = findViewById(R.id.startTestButton);
        previousReportButton = findViewById(R.id.previousReportButton);
        startChatButton = findViewById(R.id.startChatButton);

        // Get patient details from intent
        Intent intent = getIntent();
        String patientName = intent.getStringExtra("patientName");
        patientId = intent.getStringExtra("patientId");
        appointmentId = intent.getStringExtra("appointmentId");
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");

        // Get doctorId from Firebase
        doctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Set text values
        patientNameText.setText("Patient: " + patientName);
        appointmentDateText.setText("Date: " + date);
        appointmentTimeText.setText("Time: " + time);

        // Start Chat Button Click Event
        startChatButton.setOnClickListener(v -> {
            Intent chatIntent = new Intent(PatientDetailsActivity.this, ChatActivity.class);
            chatIntent.putExtra("doctorId", doctorId);
            chatIntent.putExtra("patientId", patientId);
            startActivity(chatIntent);
        });

        startTestButton.setOnClickListener(v->{
            DatabaseReference testRef = FirebaseDatabase.getInstance().getReference("testRequests").child(appointmentId);
            testRef.setValue("pending");

            Toast.makeText(PatientDetailsActivity.this, "Test Request Sent to patient", Toast.LENGTH_SHORT).show();
        });
    }
}
