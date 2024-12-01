package org.project24.audiometry;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import static org.project24.audiometry.PerformTest.testFrequencies;

public class TestData extends AppCompatActivity {
    int index;
    String[] allSavedTests;
    double[][] testResults = new double[2][testFrequencies.length];
    double[] calibrationArray = new double[testFrequencies.length];
    String fileName;
    private final float YMIN = -20f;
    private final float YMAX = 100f;
    private Context context;
    private LineChart chart;
    private boolean zoomed = false;

    public float scaleCbr(double cbr) {
        return (float) (Math.log10(cbr/125)/Math.log10(2));
    }

    public float unScaleCbr(double cbr) {
        double calcVal = Math.pow(2,cbr)*125;
        return (float)(calcVal);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        allSavedTests=TestLookup.getAllSavedTests(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.green,getTheme()));

        setContentView(R.layout.activity_test_data);
        Intent intent = getIntent();
        index = intent.getIntExtra("Index",0);
        fileName = allSavedTests[index];

        ImageButton next = (ImageButton) findViewById(R.id.next);
        next.setOnClickListener(view -> {
            index = (index - 1);
            if (index < 0) index = 0;
            fileName = allSavedTests[index];
            zoomed = false;
            draw();
        });

        ImageButton prev = (ImageButton) findViewById(R.id.prev);
        prev.setOnClickListener(view -> {
            index = (index + 1);
            if (index > allSavedTests.length-1) index = allSavedTests.length-1;
            fileName = allSavedTests[index];
            zoomed = false;
            draw();
        });

        draw();

    }

    private String getHearingLevel(double[] thresholds) {
        double average = 0.0;
        for (double threshold : thresholds) {
            average += threshold;
        }
        average /= thresholds.length;

        if (average <= 15) {
            return "Normal Hearing Capability";
        } else if (average > 15 && average <= 25) {
            return "Slight Hearing Loss";
        } else if (average > 25 && average <= 40) {
            return "Mild Hearing Loss";
        } else if (average > 40 && average <= 70) {
            return "Moderate Hearing Loss. Please Consult a Doctor";
        } else {
            return "Extreme Hearing Loss. Please Consult a Doctor";
        }
    }


    private void draw() {
        String[] names = fileName.split("-");
        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Long.parseLong(names[1])) + ", " + DateFormat.getDateInstance(DateFormat.SHORT).format(Long.parseLong(names[1]));

        ImageButton share = (ImageButton) findViewById(R.id.share_button);
        share.setOnClickListener(view -> {
            String testdata = "Thresholds right\n";
            for (int i=0; i<testFrequencies.length;i++){
                testdata+=testFrequencies[i] + " Hz " + String.format("%.1f",(float) (testResults[0][i]-calibrationArray[i])) + " dBHL\n";
            }
            testdata+="\nThresholds left\n";
            for (int i=0; i<testFrequencies.length;i++){
                testdata+=testFrequencies[i] + " Hz " + String.format("%.1f",(float) (testResults[1][i]-calibrationArray[i])) + " dBHL\n";
            }
            testdata+="\n";
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_TEXT, testdata);
            startActivity(Intent.createChooser(sharingIntent, "Share in..."));
        });

        ImageButton download = findViewById(R.id.download_button);
        download.setOnClickListener(view-> {
            exportAsPdf();
        });

        ImageButton upload = findViewById(R.id.upload_button);
        upload.setOnClickListener(view->{
            uploadTestDataToFirebase();
        });

        ImageButton zoom = findViewById(R.id.zoom_button);
        zoom.setImageDrawable(zoomed ? ContextCompat.getDrawable(this,R.drawable.ic_zoom_out_black_24dp) : ContextCompat.getDrawable(this,R.drawable.ic_zoom_in_black_24dp));
        zoom.setOnClickListener(view -> {
            if (!zoomed){
                chart.getAxisLeft().resetAxisMaximum();
                chart.getAxisLeft().resetAxisMinimum();
                chart.getAxisRight().resetAxisMaximum();
                chart.getAxisRight().resetAxisMinimum();
                zoom.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_zoom_out_black_24dp));
                zoomed = true;
            } else {
                chart.getAxisLeft().setAxisMinimum(YMIN);
                chart.getAxisLeft().setAxisMaximum(YMAX);
                chart.getAxisRight().setAxisMinimum(YMIN);
                chart.getAxisRight().setAxisMaximum(YMAX);
                zoom.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_zoom_in_black_24dp));
                zoomed = false;
            }
            chart.notifyDataSetChanged();
            chart.invalidate();
        });

        FileOperations fileOperations = new FileOperations();
        testResults=fileOperations.readTestData(fileName, context);
        calibrationArray=fileOperations.readCalibration(context);

        ImageButton delete = (ImageButton) findViewById(R.id.delete_button);
        delete.setOnClickListener(view -> {
            fileOperations.deleteTestData(fileName,context);
            allSavedTests=TestLookup.getAllSavedTests(this);
            if (index > allSavedTests.length-1) index = allSavedTests.length-1;
            fileName = allSavedTests[index];
            zoomed = false;
            draw();});

        TextView title = (TextView) findViewById(R.id.test_title);
        title.setText(time);

        // Draw Graph
        chart = (LineChart) findViewById(R.id.chart);
        chart.setExtraTopOffset(10);
        chart.setNoDataText("Whoops! No data was found. Try again!");
        Description description = new Description();
        description.setText(getResources().getString(R.string.chart_description));
        description.setTextSize(15);
        description.setTextColor(getResources().getColor(R.color.white,getTheme()));
        chart.setDescription(description);

        ArrayList<Entry> dataLeft = new ArrayList<Entry>();
        for (int i = 0; i < testResults[1].length; i ++){
            Entry dataPoint = new Entry( scaleCbr(testFrequencies[i]),(float) (testResults[1][i]-calibrationArray[i]) );
            dataLeft.add(dataPoint);
        }
        LineDataSet setLeft = new LineDataSet(dataLeft, getString(R.string.left));
        setLeft.setCircleColor(getResources().getColor(R.color.green,getTheme()));
        setLeft.setColor(getResources().getColor(R.color.green,getTheme()));
        setLeft.setValueTextColor(Color.WHITE);
        setLeft.setValueTextSize(12);

        ArrayList<Entry> dataRight = new ArrayList<Entry>();
        for (int i = 0; i < testResults[0].length; i ++){
            Entry dataPoint = new Entry( scaleCbr(testFrequencies[i]), (float)(testResults[0][i]-calibrationArray[i]));
            dataRight.add(dataPoint);
        }
        LineDataSet setRight = new LineDataSet(dataRight, getString(R.string.right));
        setRight.setCircleColor(getResources().getColor(R.color.primary_dark,getTheme()));
        setRight.setColor(getResources().getColor(R.color.primary_dark,getTheme()));
        setRight.setValueTextColor(Color.BLACK);
        setRight.setValueTextSize(12);

        LineData data = new LineData(setLeft,setRight);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.BLACK);
        xAxis.setTextSize(15);
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(YMIN);
        leftAxis.setAxisMaximum(YMAX);
        leftAxis.setTextSize(15);
        leftAxis.setInverted(true);
        leftAxis.setTextColor(Color.BLACK);
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setAxisMinimum(YMIN);
        rightAxis.setAxisMaximum(YMAX);
        rightAxis.setInverted(true);
        rightAxis.setTextSize(15);
        rightAxis.setTextColor(Color.BLACK);
        Legend legend = chart.getLegend();
        legend.setTextColor(Color.BLACK);
        legend.setTextSize(15);

        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                DecimalFormat mFormat;
                mFormat = new DecimalFormat("##0.#"); // use one decimal.
                    return mFormat.format(unScaleCbr(value));
            }
            @Override
            public int getDecimalDigits() {
                return 0;
            }
        });

        // Calculate hearing levels
        String leftHearingLevel = getHearingLevel(testResults[1]);
        String rightHearingLevel = getHearingLevel(testResults[0]);

        // Display the levels in the title or a TextView
        TextView leftLevelText = findViewById(R.id.left_level);
        TextView rightLevelText = findViewById(R.id.right_level);

        leftLevelText.setText("Left Ear: " + leftHearingLevel);
        rightLevelText.setText("Right Ear: " + rightHearingLevel);

        chart.setData(data);
        chart.invalidate(); // refresh
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.test_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            case android.R.id.home:
                gotoExport();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void gotoExport(){
        Intent intent = new Intent(this, TestLookup.class);
        startActivity(intent);
    }

    public void exportAsPdf(){

        PdfDocument pdfDocument = new PdfDocument();


        int chartWidth = chart.getWidth();
        int chartHeight = chart.getHeight();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(chartWidth, chartHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas =  page.getCanvas();


        chart.draw(canvas);

        pdfDocument.finishPage(page);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File pdfFile = new File(downloadsDir, "TestGraph_" + System.currentTimeMillis() + ".pdf");

        File file = new File(context.getExternalFilesDir(null), "Audiogram.pdf");
        try {
            pdfFile.getParentFile().mkdirs();
            FileOutputStream outputStream = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(outputStream);
            pdfDocument.close();
            Toast.makeText(this, "PDF saved to Downloads folder: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        // Close the PDF document
//        pdfDocument.close();
    }


    private void uploadTestDataToFirebase() {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Prepare test result data
        String testDate = fileName.split("-")[1]; // Extract date from fileName
        ArrayList<Double> thresholdsLeft = new ArrayList<>();
        ArrayList<Double> thresholdsRight = new ArrayList<>();
        ArrayList<Integer> frequencies = new ArrayList<>();

        for (int frequency : testFrequencies) {
            frequencies.add(frequency);
        }

        for (int i = 0; i < testFrequencies.length; i++) {
            thresholdsLeft.add(testResults[1][i] - calibrationArray[i]);
            thresholdsRight.add(testResults[0][i] - calibrationArray[i]);
        }

        // Create a map for storing data in Realtime Database
        HashMap<String, Object> testResultData = new HashMap<>();
        testResultData.put("testDate", testDate);
        testResultData.put("frequencies", frequencies);
        testResultData.put("thresholdsLeft", thresholdsLeft);
        testResultData.put("thresholdsRight", thresholdsRight);
        testResultData.put("hearingLevelLeft", getHearingLevel(testResults[1]));
        testResultData.put("hearingLevelRight", getHearingLevel(testResults[0]));

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        database.child("users").child(userId).child("testResults").push()
                .setValue(testResultData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Test result uploaded successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

    }

}
