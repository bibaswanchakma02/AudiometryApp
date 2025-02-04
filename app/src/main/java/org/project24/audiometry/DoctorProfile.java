package org.project24.audiometry;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DoctorProfile extends AppCompatActivity {

    private TextView doctorNameText;
    private Button bookAppointmentBtn;
    private DatabaseReference databaseReference;
    private String doctorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        doctorNameText = findViewById(R.id.doctorNameText);
        bookAppointmentBtn = findViewById(R.id.bookAppointmentBtn);

        // Get the doctor name from intent
        doctorName = getIntent().getStringExtra("doctorName");
        doctorNameText.setText(doctorName);

        // Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Appointments");

        bookAppointmentBtn.setOnClickListener(view -> bookAppointment());
    }

    private void bookAppointment() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String appointmentId = databaseReference.push().getKey(); // Generate unique ID

        Appointment appointment = new Appointment(userId, doctorName, "Pending");
        databaseReference.child(appointmentId).setValue(appointment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(DoctorProfile.this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DoctorProfile.this, "Failed to book appointment", Toast.LENGTH_SHORT).show();
                });
    }

    public static class Appointment {
        public String patientId;
        public String doctorName;
        public String status;

        public Appointment() {
        }

        public Appointment(String patientId, String doctorName, String status) {
            this.patientId = patientId;
            this.doctorName = doctorName;
            this.status = status;
        }
    }
}
