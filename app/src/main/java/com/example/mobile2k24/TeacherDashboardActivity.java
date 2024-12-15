package com.example.mobile2k24;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.util.Base64;
import android.net.Uri;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import android.content.ActivityNotFoundException;
import java.util.Calendar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.Query;
import com.example.mobile2k24.adapters.AbsenceAdapter;
import com.example.mobile2k24.models.Absence;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TeacherDashboardActivity extends AppCompatActivity {
    private TextView welcomeText;
    private TextView emailText;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView totalAbsencesCount;
    private TextView monthlyAbsencesCount;
    private TextView monthlyScoreText;
    private RecyclerView absencesRecyclerView;
    private AbsenceAdapter absenceAdapter;
    private FloatingActionButton fabSchedule;
    private FloatingActionButton fabReclamation;
    private List<Absence> unexcusedAbsences = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        welcomeText = findViewById(R.id.welcome_text);
        emailText = findViewById(R.id.email_text);
        logoutButton = findViewById(R.id.logout_button);

        // Initialize new views
        totalAbsencesCount = findViewById(R.id.total_absences_count);
        monthlyAbsencesCount = findViewById(R.id.monthly_absences_count);
        monthlyScoreText = findViewById(R.id.monthly_score_text);

        // Initialize RecyclerView
        absencesRecyclerView = findViewById(R.id.absences_recycler_view);
        absenceAdapter = new AbsenceAdapter();
        absencesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        absencesRecyclerView.setAdapter(absenceAdapter);

        // Initialize FAB and set click listener
        fabSchedule = findViewById(R.id.fab_schedule);
        if (fabSchedule != null) {
            Log.d("TeacherDashboard", "FAB initialized");
            fabSchedule.setOnClickListener(view -> {
                Log.d("TeacherDashboard", "FAB clicked");
                Toast.makeText(this, "Loading schedule...", Toast.LENGTH_SHORT).show();
                loadAndOpenSchedule();
            });
        } else {
            Log.e("TeacherDashboard", "FAB not found in layout");
        }

        fabReclamation = findViewById(R.id.fab_reclamation);
        fabReclamation.setOnClickListener(v -> showReclamationDialog());

        // Set up profile data
        setupProfileData();

        // Set up logout button
        logoutButton.setOnClickListener(v -> handleLogout());

        // Load statistics
        loadTeacherStatistics();
    }

    private void setupProfileData() {
        // Get user data from AuthContext
        String userName = AuthContext.getInstance().getName();
        String userEmail = AuthContext.getInstance().getEmail();

        // Set welcome text with username
        welcomeText.setText("Welcome " + (userName != null && !userName.isEmpty() ? userName : "Teacher") + "!");

        // Set email
        if (userEmail != null && !userEmail.isEmpty()) {
            emailText.setText(userEmail);
        }
    }

    private void handleLogout() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Clear AuthContext
        AuthContext.getInstance().clear();
        
        // Redirect to login screen
        Intent intent = new Intent(TeacherDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadAndOpenSchedule() {
        String teacherName = AuthContext.getInstance().getName();
        Log.d("TeacherSchedule", "Loading schedule for teacher: " + teacherName);
        Toast.makeText(this, "Searching schedule for: " + teacherName, Toast.LENGTH_SHORT).show();

        if (teacherName == null || teacherName.isEmpty()) {
            Toast.makeText(this, "Error: Teacher name not found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("teacherSchedules")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d("TeacherSchedule", "Found " + queryDocumentSnapshots.size() + " total schedules");
                
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String docTeacherName = doc.getString("teacherName");
                    String status = doc.getString("status");
                    String pdfLink = doc.getString("pdfLink");
                    
                    Log.d("TeacherSchedule", "Checking document - Teacher: " + docTeacherName 
                          + ", Status: " + status + ", Link: " + pdfLink);

                    if (docTeacherName != null && docTeacherName.equals(teacherName) 
                        && "active".equals(status)) {
                        
                        if (pdfLink != null && !pdfLink.isEmpty()) {
                            Toast.makeText(this, "Opening schedule...", Toast.LENGTH_SHORT).show();
                            
                            try {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                                browserIntent.setData(Uri.parse(pdfLink));
                                startActivity(browserIntent);
                            } catch (ActivityNotFoundException e) {
                                Log.e("TeacherSchedule", "No browser found: ", e);
                                Toast.makeText(this, "No browser app found to open the link", 
                                             Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Log.e("TeacherSchedule", "Error opening link: ", e);
                                Toast.makeText(this, "Error opening link: " + e.getMessage(), 
                                             Toast.LENGTH_LONG).show();
                            }
                            return;
                        }
                    }
                }
                
                Toast.makeText(this, "No active schedule found for " + teacherName, 
                             Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e("TeacherSchedule", "Error loading schedule: " + e.getMessage());
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadTeacherStatistics() {
        String teacherName = AuthContext.getInstance().getName();
        
        Log.d("TeacherDashboard", "Loading statistics for teacher: " + teacherName);

        if (teacherName == null || teacherName.isEmpty()) {
            Log.e("TeacherDashboard", "Teacher name is null or empty");
            Toast.makeText(this, "Error: Teacher name not found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("absences")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                final List<Absence> absenceList = new ArrayList<>();
                final int[] totalAbsences = {0};
                final int[] monthlyAbsences = {0};

                Calendar calendar = Calendar.getInstance();
                int currentMonth = calendar.get(Calendar.MONTH) + 1;
                int currentYear = calendar.get(Calendar.YEAR);

                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    try {
                        String docTeacherName = doc.getString("teacherName");
                        if (docTeacherName != null && docTeacherName.equals(teacherName)) {
                            Absence absence = new Absence();
                            absence.setId(doc.getId());
                            absence.setClassName(doc.getString("class"));
                            
                            Long recordedAt = doc.getLong("recordedAt");
                            if (recordedAt != null) {
                                absence.setRecordedAt(recordedAt);
                            }
                            
                            Long duration = doc.getLong("duration");
                            if (duration != null) {
                                absence.setDuration(duration.intValue());
                            }
                            
                            absence.setStartTime(doc.getString("startTime"));
                            absence.setEndTime(doc.getString("endTime"));
                            absence.setStatus(doc.getString("status"));
                            
                            absenceList.add(absence);
                            totalAbsences[0]++;

                            Calendar absenceDate = Calendar.getInstance();
                            absenceDate.setTimeInMillis(absence.getRecordedAt());
                            if (absenceDate.get(Calendar.MONTH) + 1 == currentMonth &&
                                absenceDate.get(Calendar.YEAR) == currentYear) {
                                monthlyAbsences[0]++;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("TeacherDashboard", "Error processing document: ", e);
                    }
                }

                // Sort the list by recordedAt (most recent first)
                absenceList.sort((a1, a2) -> Long.compare(a2.getRecordedAt(), a1.getRecordedAt()));

                // Calculate score
                int maxMonthlyAbsences = 5;
                final int score = Math.max(0, 100 - (monthlyAbsences[0] * (100 / maxMonthlyAbsences)));

                // Update UI
                runOnUiThread(() -> {
                    totalAbsencesCount.setText(String.valueOf(totalAbsences[0]));
                    monthlyAbsencesCount.setText(String.valueOf(monthlyAbsences[0]));
                    monthlyScoreText.setText(score + "%");
                    absenceAdapter.setAbsences(absenceList);
                });
                
                Log.d("TeacherDashboard", "Loaded " + totalAbsences[0] + " absences for " + teacherName);
            })
            .addOnFailureListener(e -> {
                Log.e("TeacherDashboard", "Error loading absences: ", e);
                Toast.makeText(this, "Error loading absences: " + e.getMessage(), 
                             Toast.LENGTH_SHORT).show();
            });
    }

    private void showReclamationDialog() {
        // Filter unexcused absences
        unexcusedAbsences.clear();
        for (Absence absence : absenceAdapter.getAbsences()) {
            if ("unexcused".equalsIgnoreCase(absence.getStatus())) {
                unexcusedAbsences.add(absence);
            }
        }

        if (unexcusedAbsences.isEmpty()) {
            Toast.makeText(this, "No unexcused absences to claim", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reclamation, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        Spinner spinnerAbsences = dialogView.findViewById(R.id.spinner_absences);
        EditText editMessage = dialogView.findViewById(R.id.edit_message);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit);

        // Setup spinner
        ArrayAdapter<Absence> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, unexcusedAbsences);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAbsences.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String message = editMessage.getText().toString().trim();
            if (message.isEmpty()) {
                editMessage.setError("Please enter a message");
                return;
            }

            Absence selectedAbsence = (Absence) spinnerAbsences.getSelectedItem();
            submitReclamation(selectedAbsence, message);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void submitReclamation(Absence absence, String message) {
        String teacherId = AuthContext.getInstance().getUserId();
        String teacherName = AuthContext.getInstance().getName();

        // First create the notification document
        Map<String, Object> notification = new HashMap<>();
        notification.put("absenceId", absence.getId());
        notification.put("className", absence.getClassName());
        notification.put("date", new Date().getTime());
        notification.put("message", message);
        notification.put("status", "pending");
        notification.put("teacherId", teacherId);
        notification.put("teacherName", teacherName);
        notification.put("type", "reclamation");

        // Start a batch write
        db.runBatch(batch -> {
            // Add the notification
            batch.set(db.collection("notifications").document(), notification);
            
            // Update the absence status to pending
            batch.update(db.collection("absences").document(absence.getId()), 
                        "status", "pending");
        })
        .addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Reclamation submitted successfully", 
                         Toast.LENGTH_SHORT).show();
            
            // Update the local absence object and refresh the UI
            absence.setStatus("pending");
            absenceAdapter.notifyDataSetChanged();
        })
        .addOnFailureListener(e -> {
            Toast.makeText(this, "Error submitting reclamation: " + e.getMessage(), 
                         Toast.LENGTH_SHORT).show();
        });
    }
}
