package org.project24.audiometry;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DoctorList extends AppCompatActivity {

    private ListView doctorListView;
    private ArrayAdapter<String> adapter;
    private List<String> doctorNames;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_list);

        doctorListView = findViewById(R.id.doctorListView);
        doctorNames = new ArrayList<>();
        adapter = new DoctorListAdapter(this, doctorNames);
        doctorListView.setAdapter(adapter);

        // Initialize Firebase Database Reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Query to fetch users with role "doctor"
        databaseReference.orderByChild("role").equalTo("Doctor").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("DoctorList", "Data fetched: " + dataSnapshot.toString());
                doctorNames.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String name = snapshot.child("fullname").getValue(String.class);
                    if (name != null) {
                        doctorNames.add(name);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DoctorList", "Database error: " + databaseError.getMessage());
                Toast.makeText(DoctorList.this, "Failed to fetch list of doctors.", Toast.LENGTH_SHORT).show();
            }
        });

        /*doctorListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDoctor = doctorNames.get(position);

            // Create an Intent to open DoctorProfile activity
            Intent intent = new Intent(DoctorList.this, DoctorProfile.class);
            intent.putExtra("doctorName", selectedDoctor);
            startActivity(intent);
        });*/
    }
}