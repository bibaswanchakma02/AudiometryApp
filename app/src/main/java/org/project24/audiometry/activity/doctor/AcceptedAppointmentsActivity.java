package org.project24.audiometry.activity.doctor;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;

import org.project24.audiometry.activity.patient.Appointment;
import org.project24.audiometry.activity.patient.AppointmentsAdapter;
import org.project24.audiometry.R;

import java.util.ArrayList;
import java.util.List;

public class AcceptedAppointmentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AppointmentsAdapter adapter;
    private List<Appointment> acceptedAppointmentsList;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accepted_appointments);

        recyclerView = findViewById(R.id.recyclerViewAcceptedAppointment);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        acceptedAppointmentsList = new ArrayList<>();
        adapter = new AppointmentsAdapter(this, acceptedAppointmentsList, true);
        recyclerView.setAdapter(adapter);

        databaseRef = FirebaseDatabase.getInstance().getReference("Appointments");

        fetchAcceptedAppointments();
    }

    private void fetchAcceptedAppointments() {
        databaseRef.orderByChild("status").equalTo("Accepted")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        acceptedAppointmentsList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Appointment appointment = dataSnapshot.getValue(Appointment.class);
                            if (appointment != null) {
                                acceptedAppointmentsList.add(appointment);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AcceptedAppointmentsActivity.this, "Failed to load data!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
