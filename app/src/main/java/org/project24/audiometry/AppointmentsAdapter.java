package org.project24.audiometry;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.ViewHolder> {

    private Context context;
    private List<Appointment> appointmentList;
    private DatabaseReference appointmentsRef;
    private boolean isAcceptedList;

    public AppointmentsAdapter(Context context, List<Appointment> appointmentList, boolean isAcceptedList) {
        this.context = context;
        this.appointmentList = appointmentList;
        this.appointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
        this.isAcceptedList = isAcceptedList;
    }


    @NonNull
    @Override
    public AppointmentsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.appointment_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentsAdapter.ViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);

        holder.patientNameText.setText("Patient: " + appointment.getPatientName());
        holder.dateText.setText("Date: " + appointment.getDate());
        holder.timeText.setText("Time: " + appointment.getTime());
        holder.statusText.setText("Status: " + appointment.getStatus());

        if (isAcceptedList) {
            // If viewing accepted list, hide Accept and Reject buttons
            holder.acceptButton.setVisibility(View.GONE);
            holder.rejectButton.setVisibility(View.GONE);
        } else {
            // Show Accept and Reject buttons for pending appointments
            holder.acceptButton.setOnClickListener(v -> updateAppointmentStatus(appointment, "Accepted", ""));
            holder.rejectButton.setOnClickListener(v -> showRejectionDialog(appointment, position));
        }

    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView patientNameText, dateText, timeText, statusText;
        Button acceptButton, rejectButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            patientNameText = itemView.findViewById(R.id.patientNameText);
            dateText = itemView.findViewById(R.id.dateText);
            timeText = itemView.findViewById(R.id.timeText);
            statusText = itemView.findViewById(R.id.statusText);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }

    private void updateAppointmentStatus(Appointment appointment, String status, String rejectionMessage) {
        appointment.setStatus(status);
        appointment.setRejectionMessage(rejectionMessage);

        // Update Firebase
        appointmentsRef.child(appointment.getAppointmentId()).setValue(appointment);

        // Remove from list if rejected
        if (status.equals("Rejected")) {
            appointmentList.remove(appointment);
            notifyDataSetChanged();
        }
    }
    private void showRejectionDialog(Appointment appointment, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reject_appointment, null);
        builder.setView(dialogView);

        EditText rejectionReason = dialogView.findViewById(R.id.rejectionReason);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String reason = rejectionReason.getText().toString().trim();
            if (!reason.isEmpty()) {
                updateAppointmentStatus(appointment, "Rejected", reason);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}

