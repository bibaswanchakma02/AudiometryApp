package org.project24.audiometry;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class PatientDetailsActivity extends AppCompatActivity {

    private TextView patientNameText, appointmentDateText, appointmentTimeText;
    private Button startTestButton, previousReportButton;
    private String patientId, appointmentId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        patientNameText = findViewById(R.id.patientNameText);
        appointmentDateText = findViewById(R.id.appointmentDateText);
        appointmentTimeText = findViewById(R.id.appointmentTimeText);
        startTestButton = findViewById(R.id.startTestButton);
        previousReportButton = findViewById(R.id.previousReportButton);

        // Get patient details from intent
        Intent intent = getIntent();
        String patientName = intent.getStringExtra("patientName");
        patientId = intent.getStringExtra("patientId");
        appointmentId = intent.getStringExtra("appointmentId");
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");

        // Set text values
        patientNameText.setText("Patient: " + patientName);
        appointmentDateText.setText("Date: " + date);
        appointmentTimeText.setText("Time: " + time);

        // Button Click Events
/*        startTestButton.setOnClickListener(v -> {
            Intent testIntent = new Intent(PatientDetailsActivity.this, AudiometryTestActivity.class);
            testIntent.putExtra("patientId", patientId);
            startActivity(testIntent);
        });*/

      /*  previousReportButton.setOnClickListener(v -> {
            Intent reportIntent = new Intent(PatientDetailsActivity.this, PreviousReportsActivity.class);
            reportIntent.putExtra("patientId", patientId);
            startActivity(reportIntent);
        });*/
    }
}
