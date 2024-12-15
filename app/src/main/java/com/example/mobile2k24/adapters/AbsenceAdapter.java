package com.example.mobile2k24.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.example.mobile2k24.R;
import com.example.mobile2k24.models.Absence;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AbsenceAdapter extends RecyclerView.Adapter<AbsenceAdapter.AbsenceViewHolder> {
    private List<Absence> absences = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private Context context;

    @NonNull
    @Override
    public AbsenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_absence, parent, false);
        return new AbsenceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AbsenceViewHolder holder, int position) {
        Absence absence = absences.get(position);
        MaterialCardView cardView = (MaterialCardView) holder.itemView;
        
        // Set class name
        holder.teacherName.setText(absence.getClassName());
        
        // Format and set date
        String formattedDate = getFormattedDate(absence.getRecordedAt());
        holder.date.setText(formattedDate);
        
        // Set times
        holder.startTime.setText(absence.getStartTime());
        holder.endTime.setText(" - " + absence.getEndTime());
        holder.duration.setText(absence.getDuration() + "h");
        
        // Set status with color
        String status = absence.getStatus();
        holder.status.setText(status);
        
        // Set status text color and card background based on status
        switch (status.toLowerCase()) {
            case "excused":
                holder.status.setTextColor(context.getColor(R.color.status_green));
                cardView.setCardBackgroundColor(context.getColor(R.color.light_blue));
                break;
            case "pending":
                holder.status.setTextColor(context.getColor(R.color.status_yellow));
                cardView.setCardBackgroundColor(context.getColor(R.color.light_yellow));
                break;
            case "unexcused":
                holder.status.setTextColor(context.getColor(R.color.status_red));
                if (isToday(absence.getRecordedAt())) {
                    cardView.setCardBackgroundColor(context.getColor(R.color.light_red));
                } else {
                    cardView.setCardBackgroundColor(context.getColor(android.R.color.white));
                }
                break;
            default:
                holder.status.setTextColor(context.getColor(android.R.color.darker_gray));
                cardView.setCardBackgroundColor(context.getColor(android.R.color.white));
                break;
        }
    }

    private String getFormattedDate(long timestamp) {
        Calendar absenceDate = Calendar.getInstance();
        absenceDate.setTimeInMillis(timestamp);
        
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        if (isSameDay(absenceDate, today)) {
            return "Aujourd'hui";
        } else if (isSameDay(absenceDate, yesterday)) {
            return "Hier";
        } else {
            return dateFormat.format(new Date(timestamp));
        }
    }

    private boolean isToday(long timestamp) {
        Calendar absenceDate = Calendar.getInstance();
        absenceDate.setTimeInMillis(timestamp);
        return isSameDay(absenceDate, Calendar.getInstance());
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public int getItemCount() {
        return absences.size();
    }

    public void setAbsences(List<Absence> absences) {
        this.absences = absences;
        notifyDataSetChanged();
    }

    public List<Absence> getAbsences() {
        return absences;
    }

    static class AbsenceViewHolder extends RecyclerView.ViewHolder {
        TextView teacherName, date, startTime, endTime, duration, status;

        AbsenceViewHolder(View itemView) {
            super(itemView);
            teacherName = itemView.findViewById(R.id.text_teacher_name);
            date = itemView.findViewById(R.id.text_date);
            startTime = itemView.findViewById(R.id.text_start_time);
            endTime = itemView.findViewById(R.id.text_end_time);
            duration = itemView.findViewById(R.id.text_duration);
            status = itemView.findViewById(R.id.text_status);
        }
    }
} 