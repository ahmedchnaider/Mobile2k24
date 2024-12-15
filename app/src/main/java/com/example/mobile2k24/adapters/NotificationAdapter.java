package com.example.mobile2k24.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile2k24.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Map<String, Object>> notifications;
    private Context context;
    private FirebaseFirestore db;

    public NotificationAdapter(List<Map<String, Object>> notifications, Context context) {
        this.notifications = notifications;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Map<String, Object> notification = notifications.get(position);
        
        // Set teacher name
        holder.teacherName.setText(String.valueOf(notification.get("teacherName")));
        
        // Format and set date
        Long dateTimestamp = (Long) notification.get("date");
        if (dateTimestamp != null) {
            holder.absenceDate.setText(getFormattedDate(dateTimestamp));
        }
        
        // Set message
        holder.reclamationMessage.setText(String.valueOf(notification.get("message")));

        // Set status with appropriate color
        String status = (String) notification.get("status");
        holder.statusText.setText(status.toUpperCase());
        switch (status) {
            case "approved":
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_green_dark));
                holder.buttonsLayout.setVisibility(View.GONE);
                break;
            case "rejected":
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_red_dark));
                holder.buttonsLayout.setVisibility(View.GONE);
                break;
            case "pending":
                holder.statusText.setTextColor(context.getColor(android.R.color.holo_orange_dark));
                holder.buttonsLayout.setVisibility(View.VISIBLE);
                // Set up button click listeners
                holder.btnApprove.setOnClickListener(v -> handleReclamation(notification, "approved"));
                holder.btnDeny.setOnClickListener(v -> handleReclamation(notification, "rejected"));
                break;
        }
    }

    private String getFormattedDate(long timestamp) {
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp);
        
        if (isSameDay(date, today)) {
            return "Aujourd'hui " + new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
        } else if (isSameDay(date, yesterday)) {
            return "Hier " + new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
        } else {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void handleReclamation(Map<String, Object> notification, String newStatus) {
        String notificationId = (String) notification.get("id");
        String absenceId = (String) notification.get("absenceId");
        
        Log.d("NotificationAdapter", "Processing reclamation");
        Log.d("NotificationAdapter", "NotificationID: " + notificationId);
        Log.d("NotificationAdapter", "AbsenceID: " + absenceId);
        Log.d("NotificationAdapter", "New Status: " + newStatus);

        if (absenceId == null || absenceId.isEmpty()) {
            Toast.makeText(context, "Error: Absence ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get references to both documents
        DocumentReference notificationRef = db.collection("notifications").document(notificationId);
        DocumentReference absenceRef = db.collection("absences").document(absenceId);

        // Create a batch write
        WriteBatch batch = db.batch();

        // Update notification status
        batch.update(notificationRef, "status", newStatus);

        // Update absence status based on admin's decision
        String absenceStatus = "approved".equals(newStatus) ? "excused" : "unexcused";
        batch.update(absenceRef, "status", absenceStatus);

        // Commit both updates atomically
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                Log.d("NotificationAdapter", "Successfully updated both documents");
                Log.d("NotificationAdapter", "Notification status: " + newStatus);
                Log.d("NotificationAdapter", "Absence status: " + absenceStatus);

                // Update local data
                notification.put("status", newStatus);
                
                String message = "approved".equals(newStatus) 
                    ? "Reclamation approved and absence marked as excused"
                    : "Reclamation denied and absence marked as unexcused";
                
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Log.e("NotificationAdapter", "Error updating documents: ", e);
                Toast.makeText(context, 
                    "Error updating status: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView teacherName, absenceDate, reclamationMessage, statusText;
        Button btnApprove, btnDeny;
        View buttonsLayout;

        NotificationViewHolder(View itemView) {
            super(itemView);
            teacherName = itemView.findViewById(R.id.teacher_name);
            absenceDate = itemView.findViewById(R.id.absence_date);
            reclamationMessage = itemView.findViewById(R.id.reclamation_message);
            statusText = itemView.findViewById(R.id.status_text);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnDeny = itemView.findViewById(R.id.btn_deny);
            buttonsLayout = itemView.findViewById(R.id.buttons_layout);
        }
    }
} 