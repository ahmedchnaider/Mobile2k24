package com.example.mobile2k24;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile2k24.adapters.NotificationAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private List<Map<String, Object>> notificationsList = new ArrayList<>();
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.notifications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter
        adapter = new NotificationAdapter(notificationsList, this);
        recyclerView.setAdapter(adapter);

        // Fetch all notifications
        fetchNotifications();
    }

    private void fetchNotifications() {
        db.collection("notifications")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                notificationsList.clear();
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Map<String, Object> notification = document.getData();
                    if (notification != null) {
                        // Store the document ID in the map
                        notification.put("id", document.getId());
                        
                        // Only add pending reclamations
                        String type = (String) notification.get("type");
                        String status = (String) notification.get("status");
                        if ("reclamation".equals(type) && "pending".equals(status)) {
                            String absenceId = (String) notification.get("absenceId");
                            Log.d("NotificationsActivity", "Found pending reclamation for absence: " + absenceId);
                            notificationsList.add(notification);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                
                if (notificationsList.isEmpty()) {
                    Toast.makeText(this, "No pending reclamations found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("NotificationsActivity", "Error fetching notifications: ", e);
                Toast.makeText(this, "Error loading notifications: " + e.getMessage(),
                             Toast.LENGTH_SHORT).show();
            });
    }
} 