package org.project24.audiometry.activity.patient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import java.io.File;
import java.util.Objects;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.project24.audiometry.activity.PerformSingleTest;
import org.project24.audiometry.activity.PerformTest;
import org.project24.audiometry.R;
import org.project24.audiometry.utils.TestLookup;
import org.project24.audiometry.activity.Login;
import org.project24.audiometry.utils.Backup;
import org.project24.audiometry.utils.FileOperations;
import org.project24.audiometry.utils.GithubStar;
import org.project24.audiometry.utils.Instructions;
import org.project24.audiometry.utils.Pre_Calibration;

import io.agora.rtm.RtmClient;


public class MainActivity extends AppCompatActivity {

    ActivityResultLauncher<Intent> mRestore;

    FirebaseAuth auth;
    Button button;
    TextView textView;
    FirebaseUser user;
    FirebaseDatabase database;
    DatabaseReference usersRef, testRequestRef;
    private RtmClient rtmClient;

//    private boolean isLoggedIn = false;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users");
        button = findViewById(R.id.logout);
        textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            String userId = user.getUid();
            fetchFullName(userId);
        }

        button.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(),Login.class);
                startActivity(intent);
                finish();
            }
        });


        getSupportActionBar().getThemedContext();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.green,getTheme()));
        checkShowInvisibleButtons();

        mRestore = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                      File intData = new File(Environment.getDataDirectory() + "//data//" + this.getPackageName());
                      if (result.getData()!=null && result.getData().getData()!=null) Backup.zipExtract(this, intData, result.getData().getData());
                      PerformTest.gain= FileOperations.readGain(this);
                      //Toast.makeText(this,"Gain: "+PerformTest.gain,Toast.LENGTH_LONG).show();
                      checkShowInvisibleButtons();
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
                    textView.setText("Welcome, " + fullname);
                } else {
                    textView.setText("Welcome, User");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to fetch user details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Listens for audiometry test requests from the doctor.
     */
    private void listenForTestRequests(){
        testRequestRef = database.getReference("testRequests").child(userId);
        testRequestRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String status = snapshot.child("status").getValue(String.class);
                    if("pending".equals(status)){
                        showTestRequestDialog();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching test requests", error.toException());
            }
        });
    }

    private void showTestRequestDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Test Request")
                .setMessage("Are you ready to start the test?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    testRequestRef.child("status").setValue("in_progress");

                    Intent intent = new Intent(this, PerformTest.class);
                    intent.putExtra("Action", "Test");
                    startActivity(intent);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    testRequestRef.child("status").setValue("declined");
                    dialog.dismiss();
                })
                .show();
    }
    private void checkShowInvisibleButtons(){
        Button startTest = findViewById(R.id.main_startTest);
        Button startSingleTest = findViewById(R.id.main_startSingleTest);
        Button testResults = findViewById(R.id.main_results);
        if (FileOperations.isCalibrated(this)) {
            startTest.setVisibility(View.VISIBLE);
            testResults.setVisibility(View.VISIBLE);
            startSingleTest.setVisibility(View.VISIBLE);
            if (GithubStar.shouldShowStarDialog(this)) GithubStar.starDialog(this,"");
        } else {
            startTest.setVisibility(View.GONE);
            testResults.setVisibility(View.GONE);
            startSingleTest.setVisibility(View.GONE);
        }
    }
    public void gotoPreCalibration(View view){
        Intent intent = new Intent(this, Pre_Calibration.class);
        startActivity(intent);
    }

    public void gotoDoctorList(View view){
        Intent intent = new Intent(this, DoctorList.class);
        startActivity(intent);
    }

    public void gotoAcceptedAppointments(View view){
        Intent intent = new Intent(this, AcceptedDoctorList.class);
        startActivity(intent);
    }

    /**
     * goes to PerformTest activity
     * @param view- current view
     */
    public void gotoTest(View view){
        Intent intent = new Intent(this, PerformTest.class);
        intent.putExtra("Action","Test");
        startActivity(intent);
    }

    /**
     * goes to PerformTest activity
     * @param view- current view
     */
    public void gotoSingleTest(View view){
        Intent intent = new Intent(this, PerformSingleTest.class);
        startActivity(intent);
    }

    /**
     * goes to ExportData activity
     * @param view- current view
     */
    public void gotoExport(View view){
        Intent intent = new Intent(this, TestLookup.class);
        startActivity(intent);
    }
    public void gotoInfo(View view){
        Intent intent = new Intent(this, Instructions.class);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefManager.getInt("user",1)==1) {
            menu.findItem(R.id.user).setIcon(R.drawable.ic_user1_36dp);
        } else {
            menu.findItem(R.id.user).setIcon(R.drawable.ic_user2_36dp);
        }
        menu.findItem(R.id.lowGain).setChecked(FileOperations.readGain(this) != PerformTest.highGain);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        File extStorage;
        File intData;
        int id = item.getItemId();
        if (id==R.id.backup) {
            FileOperations.writeGain(this);
            intData = new File(Environment.getDataDirectory()+"//data//" + this.getPackageName() + "//files//");
            extStorage = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS);
            String filesBackup = getResources().getString(R.string.app_name)+".zip";
            final File zipFileBackup = new File(extStorage, filesBackup);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.main_backup));
            builder.setPositiveButton(R.string.dialog_OK_button, (dialog, whichButton) -> {
                if (!Backup.checkPermissionStorage(this)) {
                    Backup.requestPermission(this);
                } else {
                    if (zipFileBackup.exists()){
                        if (!zipFileBackup.delete()){
                            Toast.makeText(this,getResources().getString(R.string.toast_delete), Toast.LENGTH_LONG).show();
                        }
                    }
                    try {
                        new ZipFile(zipFileBackup).addFolder(intData);
                    } catch (ZipException e) {
                        Toast.makeText(this,e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
            builder.setNegativeButton(R.string.dialog_NO_button, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }else if (id==R.id.restore){
            intData = new File(Environment.getDataDirectory() + "//data//" + this.getPackageName());
            extStorage = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS);
            String filesBackup = getResources().getString(R.string.app_name)+".zip";
            final File zipFileBackup = new File(extStorage, filesBackup);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.main_restore_message));
            builder.setPositiveButton(R.string.dialog_OK_button, (dialog, whichButton) -> {
                if (!Backup.checkPermissionStorage(this)) {
                    Backup.requestPermission(this);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setType("application/zip");
                        mRestore.launch(intent);
                    } else {
                        Backup.zipExtract(this, intData, Uri.fromFile(zipFileBackup));
                        PerformTest.gain=FileOperations.readGain(this);
                        //Toast.makeText(this,"Gain: "+PerformTest.gain,Toast.LENGTH_LONG).show();
                        checkShowInvisibleButtons();
                    }
                }
            });
            builder.setNegativeButton(R.string.dialog_NO_button, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }else if (id==R.id.lowGain){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.changeGain));
            builder.setMessage(getResources().getString(R.string.changeGainDescription));
            builder.setPositiveButton(R.string.dialog_OK_button, (dialog, whichButton) -> {
                if (item.isChecked()) PerformTest.gain=PerformTest.highGain; // item.isChecked always previous value until invalidated, so value has to be inverted
                else PerformTest.gain=PerformTest.lowGain;
                FileOperations.deleteAllFiles(this);
                FileOperations.writeGain(this);
                //Toast.makeText(this,"Gain: "+FileOperations.readGain(this),Toast.LENGTH_LONG).show();
                checkShowInvisibleButtons();
                invalidateOptionsMenu();
            });
            builder.setNegativeButton(R.string.dialog_NO_button, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();

        } else if (id==R.id.user){
            SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefManager.getInt("user",1)==1){
                item.setIcon(R.drawable.ic_user2_36dp);
                SharedPreferences.Editor editor = prefManager.edit();
                editor.putInt("user", 2);
                editor.apply();
            } else {
                item.setIcon(R.drawable.ic_user1_36dp);
                SharedPreferences.Editor editor = prefManager.edit();
                editor.putInt("user", 1);
                editor.apply();
            }
            invalidateOptionsMenu();
        } else if (item.getItemId() == R.id.menu_about) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("")));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
