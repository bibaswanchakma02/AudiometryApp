package org.project24.audiometry.activity.patient;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.project24.audiometry.R;
import org.project24.audiometry.activity.doctor.DoctorProfile;

import java.util.List;

public class DoctorListAdapter extends ArrayAdapter<String> {

    private Context context;
    private List<String> doctorNames;

    public DoctorListAdapter(Context context, List<String> doctorNames) {
        super(context, R.layout.list_item, doctorNames);
        this.context = context;
        this.doctorNames = doctorNames;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        }

        Button doctorButton = convertView.findViewById(R.id.doctorName);
        doctorButton.setText(doctorNames.get(position));

        doctorButton.setOnClickListener(view -> {
            Intent intent = new Intent(context, DoctorProfile.class);
            intent.putExtra("doctorName", doctorNames.get(position));
            context.startActivity(intent);
        });

        return convertView;
    }
}
