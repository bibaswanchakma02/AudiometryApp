package org.project24.audiometry;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class DoctorActivity extends AppCompatActivity {


    FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference usersRef;
    TextView welcomeTextView;
    Button logoutButton, viewAppointmentBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_home_screen);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users");

        welcomeTextView = findViewById(R.id.doctor_details);
        logoutButton = findViewById(R.id.logout);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            String userId = currentUser.getUid();
            fetchFullName(userId);
            welcomeTextView.setText(currentUser.getEmail());
        }
        else{
            redirectToLogin();
        }

        logoutButton.setOnClickListener(view -> {
            mAuth.signOut();
            redirectToLogin();
        });
    }

    /**
     * Fetches the user's fullname from Firebase and displays it in the TextView.
     *
     * @param userId The UID of the logged-in user.
     */

    private void fetchFullName(String userId) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String fullname = dataSnapshot.child("fullname").getValue(String.class);
                    welcomeTextView.setText("Welcome, " + fullname);
                } else {
                    welcomeTextView.setText("Welcome, User");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DoctorActivity.this, "Failed to fetch user details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void gotoAppointments(View view){
        Intent intent = new Intent(this, DoctorsAppointmentActivity.class);
        startActivity(intent);
    }

    public void gotoAcceptedPatients(View view){
        Intent intent = new Intent(this, AcceptedAppointmentsActivity.class);
        startActivity(intent);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(DoctorActivity.this, Login.class);
        startActivity(intent);
        finish();
    }
}