package com.example.mobile2k24.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile2k24.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AbsenceHistoryAdapter extends RecyclerView.Adapter<AbsenceHistoryAdapter.ViewHolder> {
    private List<Map<String, Object>> absences;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private FirebaseFirestore db;
    private Context context;

    public AbsenceHistoryAdapter(List<Map<String, Object>> absences, Context context) {
        this.absences = absences;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    public void updateData(List<Map<String, Object>> newAbsences) {
        this.absences = newAbsences;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_absence_history, parent, false);
        return new ViewHolder(view);
    }

    private String getRelativeDate(long timestamp) {
        Calendar absenceDate = Calendar.getInstance();
        absenceDate.setTimeInMillis(timestamp);
        
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        // Reset time part for comparison
        resetTimeToMidnight(absenceDate);
        resetTimeToMidnight(today);
        resetTimeToMidnight(yesterday);

        if (absenceDate.equals(today)) {
            return "Aujourd'hui";
        } else if (absenceDate.equals(yesterday)) {
            return "Hier";
        } else {
            return sdf.format(new Date(timestamp));
        }
    }

    private void resetTimeToMidnight(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            Map<String, Object> absence = absences.get(position);
            
            // Set teacher name
            String teacherName = (String) absence.get("teacherName");
            holder.teacherName.setText(teacherName != null ? teacherName : "N/A");

            // Set date using timestamp with relative date
            Long timestamp = (Long) absence.get("date");
            if (timestamp != null) {
                holder.date.setText(getRelativeDate(timestamp));
            } else {
                holder.date.setText("N/A");
            }

            // Set status with color
            String status = (String) absence.get("status");
            holder.status.setText(status != null ? status : "N/A");
            if ("excused".equalsIgnoreCase(status)) {
                holder.status.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else {
                holder.status.setTextColor(Color.parseColor("#F44336")); // Red
            }

            // Set duration with start and end time
            String startTime = (String) absence.get("startTime");
            String endTime = (String) absence.get("endTime");
            Number duration = (Number) absence.get("duration");
            
            if (startTime != null && endTime != null) {
                String durationText = String.format("%s - %s (%sh)", startTime, endTime, 
                    duration != null ? duration.toString() : "N/A");
                holder.duration.setText(durationText);
            } else if (duration != null) {
                holder.duration.setText(duration + "h");
            } else {
                holder.duration.setText("N/A");
            }

            // Get and set agent name
            String recordedById = (String) absence.get("recordedBy");
            if (recordedById != null) {
                holder.recordedBy.setText("..."); // Show loading dots
                db.collection("users")
                    .document(recordedById)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String agentName = documentSnapshot.getString("name");
                            holder.recordedBy.setText(agentName != null ? agentName : "Unknown");
                        } else {
                            holder.recordedBy.setText("Unknown");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AbsenceHistoryAdapter", "Error fetching agent name", e);
                        holder.recordedBy.setText("Unknown");
                    });
            } else {
                holder.recordedBy.setText("N/A");
            }

        } catch (Exception e) {
            Log.e("AbsenceHistoryAdapter", "Error binding data", e);
            // Set default values in case of error
            holder.teacherName.setText("Error loading data");
            holder.date.setText("N/A");
            holder.status.setText("N/A");
            holder.duration.setText("N/A");
            holder.recordedBy.setText("N/A");
        }
    }

    @Override
    public int getItemCount() {
        return absences != null ? absences.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView teacherName, date, status, duration, recordedBy;

        public ViewHolder(View itemView) {
            super(itemView);
            teacherName = itemView.findViewById(R.id.text_teacher_name);
            date = itemView.findViewById(R.id.text_date);
            status = itemView.findViewById(R.id.text_status);
            duration = itemView.findViewById(R.id.text_duration);
            recordedBy = itemView.findViewById(R.id.text_recorded_by);
        }
    }
} 