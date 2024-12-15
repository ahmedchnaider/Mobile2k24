package com.example.mobile2k24;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditUserActivity extends AppCompatActivity {
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private Spinner roleSpinner;
    private Spinner statusSpinner;
    private Button saveButton;
    private Button cancelButton;
    private FirebaseFirestore db;
    private String userId;
    private Map<String, Object> originalUserData;
    private LinearLayout teacherClassesContainer;
    private List<View> classViews = new ArrayList<>();
    private List<String> classesToDelete = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get userId from intent
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupSpinners();
        loadUserData();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.edit_text_name);
        emailEditText = findViewById(R.id.edit_text_email);
        phoneEditText = findViewById(R.id.edit_text_phone);
        roleSpinner = findViewById(R.id.spinner_role);
        statusSpinner = findViewById(R.id.spinner_status);
        saveButton = findViewById(R.id.button_save);
        cancelButton = findViewById(R.id.button_cancel);
        teacherClassesContainer = findViewById(R.id.teacher_classes_container);

        saveButton.setOnClickListener(v -> saveUserData());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        // Setup role spinner
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList("teacher", "agent"));
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        // Setup status spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList("active", "inactive"));
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
    }

    private void loadUserData() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading user data...");
        progressDialog.show();

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                originalUserData = documentSnapshot.getData();
                if (originalUserData != null) {
                    // Set basic fields
                    nameEditText.setText((String) originalUserData.get("name"));
                    emailEditText.setText((String) originalUserData.get("email"));
                    
                    // Set role spinner
                    String role = (String) originalUserData.get("role");
                    int rolePosition = Arrays.asList("teacher", "agent").indexOf(role);
                    if (rolePosition >= 0) {
                        roleSpinner.setSelection(rolePosition);
                    }

                    // Set status spinner
                    String status = (String) originalUserData.get("status");
                    int statusPosition = Arrays.asList("active", "inactive").indexOf(status);
                    if (statusPosition >= 0) {
                        statusSpinner.setSelection(statusPosition);
                    }

                    // Set additional info fields
                    Map<String, Object> additionalInfo = (Map<String, Object>) originalUserData.get("additionalInfo");
                    if (additionalInfo != null) {
                        phoneEditText.setText((String) additionalInfo.get("phone"));
                    }

                    // Load teacher classes
                    loadTeacherClasses();
                }
                progressDialog.dismiss();
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Error loading user data: " + e.getMessage(), 
                             Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void loadTeacherClasses() {
        if ("teacher".equals(originalUserData.get("role"))) {
            teacherClassesContainer.setVisibility(View.VISIBLE);
            // Clear existing views
            teacherClassesContainer.removeAllViews();
            classViews.clear();

            // Load all classes
            int classIndex = 0;
            String classKey = "class"; // First class
            String classValue = (String) originalUserData.get(classKey);
            
            while (classValue != null) {
                View classView = addClassField(classValue);
                classViews.add(classView);
                
                // Check for next class
                classIndex++;
                classKey = "class" + classIndex;
                classValue = (String) originalUserData.get(classKey);
            }
        } else {
            teacherClassesContainer.setVisibility(View.GONE);
        }
    }

    private View addClassField(String classValue) {
        View classView = getLayoutInflater().inflate(R.layout.edit_teacher_class_item, null);
        
        AutoCompleteTextView niveauSpinner = classView.findViewById(R.id.spinner_teacher_niveau);
        AutoCompleteTextView licenceSpinner = classView.findViewById(R.id.spinner_teacher_licence);
        EditText groupeEditText = classView.findViewById(R.id.edit_text_teacher_groupe);
        FloatingActionButton removeButton = classView.findViewById(R.id.fab_remove_class);
        
        // Setup spinners
        setupClassSpinners(niveauSpinner, licenceSpinner);
        
        // Parse and set existing values
        if (classValue != null && !classValue.isEmpty()) {
            String[] parts = classValue.split(" ");
            if (parts.length >= 3) {
                niveauSpinner.setText(parts[0]);
                licenceSpinner.setText(parts[1]);
                groupeEditText.setText(parts[2]);
            }
        }
        
        // Setup remove button
        removeButton.setOnClickListener(v -> {
            int index = classViews.indexOf(classView);
            if (index >= 0) {
                // Mark class for deletion in Firestore
                String classKey = index == 0 ? "class" : "class" + (index);
                classesToDelete.add(classKey);
                
                // Remove view
                teacherClassesContainer.removeView(classView);
                classViews.remove(classView);
            }
        });
        
        teacherClassesContainer.addView(classView);
        return classView;
    }

    private void setupClassSpinners(AutoCompleteTextView niveauSpinner, AutoCompleteTextView licenceSpinner) {
        String[] niveaux = new String[]{"1", "2", "3", "M1", "M2"};
        ArrayAdapter<String> niveauAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, niveaux);
        niveauSpinner.setAdapter(niveauAdapter);

        niveauSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedNiveau = niveauSpinner.getText().toString();
            updateLicenceOptions(selectedNiveau, licenceSpinner);
        });
    }

    private void updateLicenceOptions(String niveau, AutoCompleteTextView licenceSpinner) {
        String[] licences;
        switch (niveau) {
            case "1":
                licences = new String[]{"LE", "LG", "LIG"};
                break;
            case "2":
                licences = new String[]{"L COMPTA", "L IG", "LSE", "LSG"};
                break;
            case "3":
                licences = new String[]{"APE", "BE", "CPT", "FIN", "GRH", "IEF", "LIG", 
                                      "MFBA", "MGT", "MKG"};
                break;
            case "M1":
                licences = new String[]{"CCA", "CIE", "DIG FIN", "DIG MGT", "DIG MKG", "DMI", 
                                      "EDR", "ENE", "FIN PRO", "GRH", "INAE", "MEMICC", 
                                      "MFI", "MML", "MSO"};
                break;
            case "M2":
                licences = new String[]{"CCA", "CIE", "DDS", "DIG FIN", "DIG MGT", "DIG MKG", 
                                      "DMI", "EDR", "ENE", "FIN GRIM", "FIN PRO", "GRH", 
                                      "INAE", "MEMICC", "MFI", "MML", "MSO"};
                break;
            default:
                licences = new String[]{};
                break;
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, licences);
        licenceSpinner.setAdapter(adapter);
    }

    private void saveUserData() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving changes...");
        progressDialog.show();

        Map<String, Object> updates = new HashMap<>();
        
        // Only update fields that have changed
        String newName = nameEditText.getText().toString().trim();
        if (!newName.isEmpty() && !newName.equals(originalUserData.get("name"))) {
            updates.put("name", newName);
        }

        String newEmail = emailEditText.getText().toString().trim();
        if (!newEmail.isEmpty() && !newEmail.equals(originalUserData.get("email"))) {
            updates.put("email", newEmail);
        }

        String newRole = roleSpinner.getSelectedItem().toString();
        if (!newRole.equals(originalUserData.get("role"))) {
            updates.put("role", newRole);
        }

        String newStatus = statusSpinner.getSelectedItem().toString();
        if (!newStatus.equals(originalUserData.get("status"))) {
            updates.put("status", newStatus);
        }

        // Handle additionalInfo updates
        Map<String, Object> originalAdditionalInfo = (Map<String, Object>) originalUserData.get("additionalInfo");
        Map<String, Object> newAdditionalInfo = new HashMap<>();
        
        String newPhone = phoneEditText.getText().toString().trim();
        
        if (originalAdditionalInfo != null) {
            newAdditionalInfo.putAll(originalAdditionalInfo);
        }
        
        if (!newPhone.isEmpty() && !newPhone.equals(originalAdditionalInfo.get("phone"))) {
            newAdditionalInfo.put("phone", newPhone);
        }

        if (!newAdditionalInfo.equals(originalAdditionalInfo)) {
            updates.put("additionalInfo", newAdditionalInfo);
        }

        // Handle class updates for teachers
        if ("teacher".equals(newRole)) {
            // Remove deleted classes
            for (String classKey : classesToDelete) {
                updates.put(classKey, null); // This will delete the field in Firestore
            }
            
            // Update remaining classes
            for (int i = 0; i < classViews.size(); i++) {
                View classView = classViews.get(i);
                AutoCompleteTextView niveauSpinner = classView.findViewById(R.id.spinner_teacher_niveau);
                AutoCompleteTextView licenceSpinner = classView.findViewById(R.id.spinner_teacher_licence);
                EditText groupeEditText = classView.findViewById(R.id.edit_text_teacher_groupe);
                
                String niveau = niveauSpinner.getText().toString();
                String licence = licenceSpinner.getText().toString();
                String groupe = groupeEditText.getText().toString();
                
                if (!niveau.isEmpty() && !licence.isEmpty() && !groupe.isEmpty()) {
                    String classValue = String.format("%s %s %s", niveau, licence, groupe);
                    String classKey = i == 0 ? "class" : "class" + i;
                    updates.put(classKey, classValue);
                }
            }
        }

        // Add lastUpdated timestamp
        updates.put("lastUpdated", System.currentTimeMillis());

        // Only update if there are changes
        if (!updates.isEmpty()) {
            db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error updating user: " + e.getMessage(), 
                                 Toast.LENGTH_SHORT).show();
                });
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
} 