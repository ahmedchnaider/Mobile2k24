package com.example.mobile2k24;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile2k24.adapters.AgentAbsenceAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AgentDashboardActivity extends AppCompatActivity {
    private TextView welcomeText;
    private TextView emailText;
    private TextView absenceCountText;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView absencesRecyclerView;
    private AgentAbsenceAdapter absencesAdapter;
    private List<Map<String, Object>> absencesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent_dashboard);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set up profile data
        setupProfileData();

        // Set up RecyclerView
        setupRecyclerView();

        // Fetch absence count and list
        fetchAbsenceCount();
        fetchAbsences();

        // Set up logout button
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    private void initializeViews() {
        welcomeText = findViewById(R.id.welcome_text);
        emailText = findViewById(R.id.email_text);
        absenceCountText = findViewById(R.id.absence_count_text);
        logoutButton = findViewById(R.id.logout_button);
        
        // Initialize FABs
        FloatingActionButton fabAddAbsence = findViewById(R.id.fab_add_absence);
        FloatingActionButton fabManageSchedule = findViewById(R.id.fab_manage_schedule);

        // Set up FAB click listeners
        fabAddAbsence.setOnClickListener(v -> {
            Intent intent = new Intent(AgentDashboardActivity.this, AgentAddAbsenceActivity.class);
            startActivity(intent);
        });

        fabManageSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(AgentDashboardActivity.this, ScheduleManagementActivity.class);
            startActivity(intent);
        });
    }

    private void setupProfileData() {
        // Get user data from AuthContext
        String userName = AuthContext.getInstance().getName();
        String userEmail = AuthContext.getInstance().getEmail();

        // Set welcome text with username
        welcomeText.setText("Welcome " + (userName != null && !userName.isEmpty() ? userName : "Agent") + "!");

        // Set email
        if (userEmail != null && !userEmail.isEmpty()) {
            emailText.setText(userEmail);
        }
    }

    private void setupRecyclerView() {
        absencesRecyclerView = findViewById(R.id.recycler_absences);
        absencesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        absencesAdapter = new AgentAbsenceAdapter(this, absencesList);
        absencesRecyclerView.setAdapter(absencesAdapter);
    }

    private void fetchAbsenceCount() {
        String agentId = AuthContext.getInstance().getUserId();
        
        if (agentId == null) {
            Log.e("AgentDashboard", "ID de l'agent est null");
            return;
        }

        db.collection("absences")
            .whereEqualTo("recordedBy", agentId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int count = queryDocumentSnapshots.size();
                absenceCountText.setText("Absences enregistrées: " + count);
            })
            .addOnFailureListener(e -> {
                Log.e("AgentDashboard", "Erreur lors de la récupération du nombre d'absences", e);
                absenceCountText.setText("Absences enregistrées: 0");
            });
    }

    private void fetchAbsences() {
        // This method remains unchanged - fetches all absences for display
        db.collection("absences")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(50)  // Limit to most recent 50 absences
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                absencesList.clear();
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Map<String, Object> absence = document.getData();
                    if (absence != null) {
                        absence.put("id", document.getId());
                        absencesList.add(absence);
                    }
                }
                absencesAdapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Log.e("AgentDashboard", "Error fetching absences", e);
                Toast.makeText(AgentDashboardActivity.this,
                    "Error loading absences: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void handleLogout() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Clear AuthContext
        AuthContext.getInstance().clear();
        
        // Redirect to login screen
        Intent intent = new Intent(AgentDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAbsenceCount();
        fetchAbsences(); // Refresh the absences list
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is still authenticated
        if (mAuth.getCurrentUser() == null) {
            // User is not authenticated, return to login
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        // Refresh data
        fetchAbsenceCount();
        fetchAbsences();
    }
} 