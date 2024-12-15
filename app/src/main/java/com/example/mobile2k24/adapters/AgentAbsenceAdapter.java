package com.example.mobile2k24.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile2k24.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AgentAbsenceAdapter extends RecyclerView.Adapter<AgentAbsenceAdapter.AbsenceViewHolder> {
    private List<Map<String, Object>> absences;
    private Context context;
    private SimpleDateFormat dateFormat;

    public AgentAbsenceAdapter(Context context, List<Map<String, Object>> absences) {
        this.context = context;
        this.absences = absences;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public AbsenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_absence, parent, false);
        return new AbsenceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AbsenceViewHolder holder, int position) {
        Map<String, Object> absence = absences.get(position);

        // Set teacher name
        String teacherName = (String) absence.get("teacherName");
        holder.teacherNameText.setText(teacherName != null ? teacherName : "N/A");

        // Set date with relative format
        Long timestamp = (Long) absence.get("date");
        if (timestamp != null) {
            holder.dateText.setText(getRelativeDate(timestamp));
        } else {
            holder.dateText.setText("N/A");
        }

        // Set time range
        String startTime = (String) absence.get("startTime");
        String endTime = (String) absence.get("endTime");
        holder.startTimeText.setText(startTime != null ? startTime : "N/A");
        holder.endTimeText.setText(endTime != null ? endTime : "N/A");

        // Set duration
        Number duration = (Number) absence.get("duration");
        if (duration != null) {
            holder.durationText.setText(String.format(Locale.getDefault(), "%.2f h", duration.doubleValue()));
        } else {
            holder.durationText.setText("N/A");
        }

        // Set status with color
        String status = (String) absence.get("status");
        holder.statusText.setText(status != null ? status : "N/A");
        if ("excused".equalsIgnoreCase(status)) {
            holder.statusText.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            holder.statusText.setTextColor(Color.parseColor("#F44336")); // Red
        }
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
            return "Today";
        } else if (absenceDate.equals(yesterday)) {
            return "Yesterday";
        } else {
            return dateFormat.format(new Date(timestamp));
        }
    }

    private void resetTimeToMidnight(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    @Override
    public int getItemCount() {
        return absences != null ? absences.size() : 0;
    }

    public void updateData(List<Map<String, Object>> newAbsences) {
        this.absences = newAbsences;
        notifyDataSetChanged();
    }

    static class AbsenceViewHolder extends RecyclerView.ViewHolder {
        TextView teacherNameText;
        TextView dateText;
        TextView startTimeText;
        TextView endTimeText;
        TextView durationText;
        TextView statusText;

        AbsenceViewHolder(View itemView) {
            super(itemView);
            teacherNameText = itemView.findViewById(R.id.text_teacher_name);
            dateText = itemView.findViewById(R.id.text_date);
            startTimeText = itemView.findViewById(R.id.text_start_time);
            endTimeText = itemView.findViewById(R.id.text_end_time);
            durationText = itemView.findViewById(R.id.text_duration);
            statusText = itemView.findViewById(R.id.text_status);
        }
    }
} 