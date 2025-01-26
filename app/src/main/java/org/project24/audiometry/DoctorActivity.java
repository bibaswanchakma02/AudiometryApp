package org.project24.audiometry;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DoctorActivity extends AppCompatActivity {


    FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference usersRef;
    TextView welcomeTextView;
    Button logoutButton;

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

    private void redirectToLogin() {
        Intent intent = new Intent(DoctorActivity.this, Login.class);
        startActivity(intent);
        finish();
    }
}