package org.project24.audiometry;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;

public class DoctorProfile extends AppCompatActivity {

    private TextView doctorNameText;
    private Button bookAppointmentBtn;
    private DatabaseReference databaseReference;
    private String doctorName;
    private String selectedDate = "";
    private String selectedTime = "";

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

//        bookAppointmentBtn.setOnClickListener(view -> bookAppointment());
        bookAppointmentBtn.setOnClickListener(view -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            selectedDate = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
            showTimePicker();
        }, year, month, day);

        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1); // Format: HH:MM
            confirmBooking();
        }, hour, minute, true);

        timePickerDialog.show();
    }

    private void confirmBooking() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Appointment")
                .setMessage("Book appointment on " + selectedDate + " at " + selectedTime + "?")
                .setPositiveButton("Confirm", (dialog, which) -> bookAppointment(selectedDate, selectedTime))
                .setNegativeButton("Cancel", null)
                .show();
    }


//    private void bookAppointment(String selectedDate, String selectedTime) {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String userId = currentUser.getUid();
//        String appointmentId = databaseReference.push().getKey(); // Generate unique ID
//
//        Appointment appointment = new Appointment(userId, doctorName, "Pending", selectedDate, selectedTime);
//        databaseReference.child(appointmentId).setValue(appointment)
//                .addOnSuccessListener(aVoid -> {
//                    Toast.makeText(DoctorProfile.this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(DoctorProfile.this, "Failed to book appointment", Toast.LENGTH_SHORT).show();
//                });
//    }

    private void bookAppointment(String selectedDate, String selectedTime) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String patientId = currentUser.getUid();
        String patientName = (currentUser.getDisplayName() != null) ? currentUser.getDisplayName() : "Unknown patient name";
        String doctorName = getIntent().getStringExtra("doctorName");

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.orderByChild("fullname").equalTo(doctorName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                        String doctorId = doctorSnapshot.getKey();  // Get Doctor's UID
                        String appointmentId = databaseReference.push().getKey();

                        Appointment appointment = new Appointment(
                                appointmentId, patientId, doctorId, doctorName, patientName, "Pending",
                                selectedDate, selectedTime, ""
                        );

                        databaseReference.child(appointmentId).setValue(appointment)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(DoctorProfile.this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(DoctorProfile.this, "Failed to book appointment", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Toast.makeText(DoctorProfile.this, "Doctor not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorProfile.this, "Error fetching doctor details!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

