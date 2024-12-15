package com.example.mobile2k24;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleManagementActivity extends AppCompatActivity {
    private AutoCompleteTextView teacherScheduleSpinner;
    private TextInputEditText pdfLinkInput;
    private MaterialButton assignScheduleButton;
    private FirebaseFirestore db;
    private List<Map<String, Object>> teachersList = new ArrayList<>();
    private int selectedTeacherPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_schedule_management);

            db = FirebaseFirestore.getInstance();
            initializeViews();
            loadTeachers();
        } catch (Exception e) {
            Log.e("ScheduleManagement", "Error in onCreate: ", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        teacherScheduleSpinner = findViewById(R.id.spinner_teacher_schedule);
        pdfLinkInput = findViewById(R.id.edit_text_pdf_link);
        assignScheduleButton = findViewById(R.id.button_assign_schedule);

        teacherScheduleSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedTeacherPosition = position;
        });

        assignScheduleButton.setOnClickListener(v -> assignSchedule());
    }

    private void loadTeachers() {
        db.collection("users")
                .whereEqualTo("role", "teacher")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> teacherNames = new ArrayList<>();
                    teachersList.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> teacher = document.getData();
                        String teacherId = document.getId();
                        teacher.put("uid", teacherId);
                        teachersList.add(teacher);
                        teacherNames.add(teacher.get("name").toString());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            teacherNames
                    );
                    teacherScheduleSpinner.setAdapter(adapter);
                    teacherScheduleSpinner.setOnClickListener(v -> 
                        teacherScheduleSpinner.showDropDown());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading teachers: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void assignSchedule() {
        String pdfLink = pdfLinkInput.getText().toString().trim();

        if (pdfLink.isEmpty()) {
            Toast.makeText(this, "Please enter a PDF link", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTeacherPosition == -1) {
            Toast.makeText(this, "Please select a teacher", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> selectedTeacher = teachersList.get(selectedTeacherPosition);
        String selectedTeacherId = (String) selectedTeacher.get("uid");
        String selectedTeacherName = (String) selectedTeacher.get("name");

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Assigning schedule...");
        progressDialog.show();

        // Update existing schedules to inactive
        db.collection("teacherSchedules")
                .whereEqualTo("teacherId", selectedTeacherId)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Create new schedule data
                    Map<String, Object> scheduleData = new HashMap<>();
                    scheduleData.put("teacherId", selectedTeacherId);
                    scheduleData.put("teacherName", selectedTeacherName);
                    scheduleData.put("pdfLink", pdfLink);
                    scheduleData.put("assignedAt", System.currentTimeMillis());
                    scheduleData.put("assignedBy", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    scheduleData.put("status", "active");

                    // Update old schedules and add new one
                    db.runBatch(batch -> {
                        // Set old schedules to inactive
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            batch.update(doc.getReference(), "status", "inactive");
                        }
                        // Add new schedule
                        batch.set(db.collection("teacherSchedules").document(), scheduleData);
                    }).addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Schedule assigned successfully", 
                                     Toast.LENGTH_SHORT).show();
                        // Clear inputs after successful assignment
                        pdfLinkInput.setText("");
                        teacherScheduleSpinner.setText("");
                        selectedTeacherPosition = -1;
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Error assigning schedule: " + e.getMessage(), 
                                     Toast.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error updating existing schedules: " + e.getMessage(), 
                                 Toast.LENGTH_LONG).show();
                });
    }
} 