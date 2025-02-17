package org.project24.audiometry;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DoctorsAppointmentActivity extends AppCompatActivity {

    private RecyclerView appointmentsRecyclerView;
    private AppointmentsAdapter adapter;
    private List<Appointment> appointmentList;
    private DatabaseReference appointmentsRef;
    private String doctorId;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointments);

        // Initialize Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            doctorId = user.getUid();
        } else {
            Toast.makeText(this, "Authentication error!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        appointmentsRecyclerView = findViewById(R.id.appointmentsRecyclerView);
        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        appointmentList = new ArrayList<>();
        adapter = new AppointmentsAdapter(this, appointmentList, false);
        appointmentsRecyclerView.setAdapter(adapter);

        // Firebase Reference
        appointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");

        loadAppointments();
    }

    private void loadAppointments(){
        appointmentsRef.orderByChild("doctorId").equalTo(doctorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Appointment appointment = dataSnapshot.getValue(Appointment.class);
                    if (appointment != null && "Pending".equals(appointment.getStatus())) {
                        appointmentList.add(appointment);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorsAppointmentActivity.this, "Failed to load appointments.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

