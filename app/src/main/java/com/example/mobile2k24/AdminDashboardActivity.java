package com.example.mobile2k24;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile2k24.adapters.UserAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class AdminDashboardActivity extends AppCompatActivity {
    private TextView welcomeText;
    private TextView emailText;
    private TextView teacherCountText;
    private TextView agentCountText;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView usersRecyclerView;
    private UserAdapter userAdapter;
    private List<Map<String, Object>> usersList = new ArrayList<>();
    private FloatingActionButton fabAbsence, fabSchedule, fabNotification, fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set up profile data
        setupProfileData();

        // Add this debug Toast
        Toast.makeText(this, "Fetching users...", Toast.LENGTH_SHORT).show();
        
        // Fetch users for RecyclerView
        fetchUsers();

        // Set up logout button
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the users list when returning to this activity
        fetchUsers();
    }

    private void initializeViews() {
        welcomeText = findViewById(R.id.welcome_text);
        emailText = findViewById(R.id.email_text);
        teacherCountText = findViewById(R.id.teacher_count);
        agentCountText = findViewById(R.id.agent_count);
        logoutButton = findViewById(R.id.logout_button);
        usersRecyclerView = findViewById(R.id.users_recycler_view);
        fabAbsence = findViewById(R.id.fab_absence);
        fabSchedule = findViewById(R.id.fab_schedule);
        fabNotification = findViewById(R.id.fab_notification);
        fabAdd = findViewById(R.id.fab_add);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize empty list if not already initialized
        if (usersList == null) {
            usersList = new ArrayList<>();
        }
        
        userAdapter = new UserAdapter(usersList, new UserAdapter.OnUserActionListener() {
            @Override
            public void onEditUser(Map<String, Object> user) {
                showEditUserDialog(user);
            }

            @Override
            public void onDeleteUser(Map<String, Object> user) {
                showDeleteConfirmation(user);
            }
        });
        
        usersRecyclerView.setAdapter(userAdapter);
        
        setupFabListeners();
    }

    private void setupFabListeners() {
        fabAbsence.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AddAbsenceActivity.class);
            startActivity(intent);
        });

        fabSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ScheduleManagementActivity.class);
            startActivity(intent);
        });

        fabNotification.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, NotificationsActivity.class);
            startActivity(intent);
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AddUserActivity.class);
            startActivity(intent);
        });
    }

    private void setupProfileData() {
        String userName = AuthContext.getInstance().getName();
        String userEmail = AuthContext.getInstance().getEmail();

        // Set welcome text
        welcomeText.setText("Welcome " + (userName != null && !userName.isEmpty() ? userName : "Admin") + "!");

        // Set email
        if (userEmail != null && !userEmail.isEmpty()) {
            emailText.setText(userEmail);
        }
    }

    private void fetchUserCounts() {
        int teacherCount = 0;
        int agentCount = 0;
        
        for (Map<String, Object> user : usersList) {
            String role = (String) user.get("role");
            String status = (String) user.get("status");
            
            // Only count active users with teacher or agent roles
            if ("active".equals(status)) {
                if ("teacher".equals(role)) {
                    teacherCount++;
                } else if ("agent".equals(role)) {
                    agentCount++;
                }
            }
        }
        
        teacherCountText.setText("Active Teachers: " + teacherCount);
        agentCountText.setText("Active Agents: " + agentCount);
    }

    private void handleLogout() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Clear AuthContext
        AuthContext.getInstance().clear();
        
        // Redirect to login screen
        Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchUsers() {
        Log.d("AdminDashboard", "Starting fetchUsers");
        
        db.collection("users")
            .whereIn("role", Arrays.asList("teacher", "agent"))  // Only fetch teachers and agents
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                usersList.clear();
                Log.d("AdminDashboard", "Number of documents: " + queryDocumentSnapshots.size());
                
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Map<String, Object> userData = document.getData();
                    if (userData != null) {
                        String name = (String) userData.get("name");
                        String email = (String) userData.get("email");
                        String role = (String) userData.get("role");
                        String status = (String) userData.get("status");
                        
                        Log.d("AdminDashboard", "Processing user: " + name + ", Role: " + role + ", Status: " + status);
                        
                        // Add document ID to the map
                        userData.put("uid", document.getId());
                        usersList.add(userData);
                    }
                }
                
                // Update the adapter
                userAdapter.notifyDataSetChanged();
                
                // Update user counts
                fetchUserCounts();
                
                if (usersList.isEmpty()) {
                    Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("AdminDashboard", "Error fetching users", e);
                Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showEditUserDialog(Map<String, Object> user) {
        String userId = (String) user.get("uid");
        Intent intent = new Intent(AdminDashboardActivity.this, EditUserActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void showDeleteConfirmation(Map<String, Object> user) {
        String userName = (String) user.get("name");
        String userId = (String) user.get("uid");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmer la suppression")
               .setMessage("Êtes-vous sûr de vouloir supprimer l'utilisateur " + userName + " ?")
               .setPositiveButton("Supprimer", (dialog, which) -> {
                   deleteUser(userId);
               })
               .setNegativeButton("Annuler", (dialog, which) -> {
                   dialog.dismiss();
               })
               .show();
    }

    private void deleteUser(String userId) {
        // Show loading dialog
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
            .setMessage("Suppression en cours...")
            .setCancelable(false)
            .show();

        db.collection("users").document(userId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                loadingDialog.dismiss();
                Toast.makeText(this, "Utilisateur supprimé avec succès", Toast.LENGTH_SHORT).show();
                // Refresh the users list
                fetchUsers();
            })
            .addOnFailureListener(e -> {
                loadingDialog.dismiss();
                Toast.makeText(this, "Erreur lors de la suppression: " + e.getMessage(), 
                             Toast.LENGTH_LONG).show();
            });
    }
} 