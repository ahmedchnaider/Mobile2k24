package com.example.mobile2k24;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddUserActivity extends AppCompatActivity {
    // User Management Fields
    private EditText nameEditText;
    private EditText emailEditText;
    private AutoCompleteTextView roleSpinner;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;
    private FirebaseFirestore db;
    private String userId;
    private FirebaseAuth mAuth;

    // Absence Recording Fields
    private AutoCompleteTextView teacherSpinner;
    private AutoCompleteTextView statusSpinner;
    private Button dateButton;
    private Button saveAbsenceButton;
    private long selectedTimestamp;
    private EditText startTimeEditText;
    private EditText endTimeEditText;
    private EditText absenceDurationEditText;
    private int startHour = -1;
    private int startMinute = -1;
    private int endHour = -1;
    private int endMinute = -1;

    // Class Management Fields
    private AutoCompleteTextView niveauSpinner;
    private AutoCompleteTextView licenceSpinner;
    private EditText groupeEditText;
    private Button saveClassButton;
    private Button deleteClassButton;

    // Teacher Class Selection Fields
    private LinearLayout classSelectionLayout;
    private AutoCompleteTextView teacherNiveauSpinner;
    private AutoCompleteTextView teacherLicenceSpinner;
    private EditText teacherGroupeEditText;

    // Absence Class Selection Fields
    private AutoCompleteTextView absenceNiveauSpinner;
    private AutoCompleteTextView absenceLicenceSpinner;
    private EditText absenceGroupeEditText;

    // Add these fields at the class level
    private LinearLayout teacherClassesContainer;
    private FloatingActionButton fabAddClass;
    private int classCount = 0;
    private List<View> classViews = new ArrayList<>();

    private interface OnEmailCheckListener {
        void onResult(boolean exists);
    }

    private void checkEmailExists(String email, OnEmailCheckListener listener) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    listener.onResult(true);
                } else {
                    listener.onResult(false);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("EmailCheck", "Error checking email", e);
                listener.onResult(false);
            });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_add_user);

            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            // Initialize views
            initializeViews();
            
        } catch (Exception e) {
            Log.e("AddUserActivity", "Error in onCreate: ", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        try {
            // Initialize User Management views
            nameEditText = findViewById(R.id.edit_text_username);
            emailEditText = findViewById(R.id.edit_text_email);
            roleSpinner = findViewById(R.id.spinner_role);
            saveButton = findViewById(R.id.button_create_user);
            cancelButton = findViewById(R.id.button_cancel);

            // Initialize Absence Recording views
            teacherSpinner = findViewById(R.id.spinner_teacher);
            statusSpinner = findViewById(R.id.spinner_status);
            dateButton = findViewById(R.id.button_date);
            saveAbsenceButton = findViewById(R.id.button_save_absence);

            // Initialize time fields
            startTimeEditText = findViewById(R.id.edit_text_start_time);
            endTimeEditText = findViewById(R.id.edit_text_end_time);
            absenceDurationEditText = findViewById(R.id.edit_text_absence_duration);

            // Initialize class management views
            niveauSpinner = findViewById(R.id.spinner_niveau);
            licenceSpinner = findViewById(R.id.spinner_licence);
            groupeEditText = findViewById(R.id.edit_text_groupe);
            saveClassButton = findViewById(R.id.button_save_class);
            deleteClassButton = findViewById(R.id.button_delete_class);

            // Initialize teacher class selection views
            classSelectionLayout = findViewById(R.id.layout_teacher_class);
            teacherNiveauSpinner = findViewById(R.id.spinner_teacher_niveau);
            teacherLicenceSpinner = findViewById(R.id.spinner_teacher_licence);
            teacherGroupeEditText = findViewById(R.id.edit_text_teacher_groupe);

            // Initialize absence class selection views
            absenceNiveauSpinner = findViewById(R.id.spinner_absence_niveau);
            absenceLicenceSpinner = findViewById(R.id.spinner_absence_licence);
            absenceGroupeEditText = findViewById(R.id.edit_text_absence_groupe);

            // Set up spinners
            setupSpinners();

            // Set up click listeners
            setupClickListeners();

            // Set up time picker listeners
            startTimeEditText.setOnClickListener(v -> showTimePicker(true));
            endTimeEditText.setOnClickListener(v -> showTimePicker(false));

            // Setup role spinner listener
            roleSpinner.setOnItemClickListener((parent, view, position, id) -> {
                String selectedRole = roleSpinner.getText().toString();
                classSelectionLayout.setVisibility(
                    "teacher".equals(selectedRole) ? View.VISIBLE : View.GONE
                );
            });

            // Setup teacher class spinners
            setupTeacherClassSpinners();

            // Setup absence class spinners
            setupAbsenceClassSpinners();

            teacherClassesContainer = findViewById(R.id.teacher_classes_container);
            fabAddClass = findViewById(R.id.fab_add_class);
            
            fabAddClass.setOnClickListener(v -> addNewClassFields());
            
            // Add the first class view to the list
            classViews.add(teacherClassesContainer.getChildAt(0));

        } catch (Exception e) {
            Log.e("AddUserActivity", "Error in initializeViews: ", e);
            throw e;
        }
    }

    private void setupSpinners() {
        try {
            // Setup role spinner
            String[] roles = new String[]{"agent", "teacher"};
            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
            );
            roleSpinner.setAdapter(roleAdapter);

            // Setup status spinner
            String[] statuses = new String[]{"excused", "unexcused"};
            ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                statuses
            );
            statusSpinner.setAdapter(statusAdapter);

            // Load teachers for absence recording
            loadTeachers();

            // Setup class management views
            setupNiveauSpinner(niveauSpinner, licenceSpinner);
            setupClassManagementListeners();

        } catch (Exception e) {
            Log.e("AddUserActivity", "Error in setupSpinners: ", e);
            throw e;
        }
    }

    private void setupClickListeners() {
        try {
            // User save button
            saveButton.setOnClickListener(v -> saveUser());

            // Absence save button
            saveAbsenceButton.setOnClickListener(v -> {
                saveAbsence();
            });

            // Cancel button
            cancelButton.setOnClickListener(v -> finish());

            // Date button
            dateButton.setOnClickListener(v -> showDatePicker());

        } catch (Exception e) {
            Log.e("AddUserActivity", "Error in setupClickListeners: ", e);
            throw e;
        }
    }

    private void loadTeachers() {
        db.collection("users")
                .whereEqualTo("role", "teacher")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> teacherNames = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String teacherName = document.getString("name");
                        if (teacherName != null) {
                            teacherNames.add(teacherName);
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            teacherNames
                    );
                    teacherSpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading teachers: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    selectedTimestamp = selectedDate.getTimeInMillis();
                    dateButton.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute1) -> {
                    if (isStartTime) {
                        startHour = hourOfDay;
                        startMinute = minute1;
                        startTimeEditText.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1));
                    } else {
                        endHour = hourOfDay;
                        endMinute = minute1;
                        endTimeEditText.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1));
                    }
                    calculateDuration();
                },
                hour,
                minute,
                true // 24-hour format
        );
        timePickerDialog.show();
    }

    private void calculateDuration() {
        if (startHour != -1 && endHour != -1) {
            float startTime = startHour + (startMinute / 60.0f);
            float endTime = endHour + (endMinute / 60.0f);
            
            float duration;
            if (endTime >= startTime) {
                duration = endTime - startTime;
            } else {
                // If end time is on the next day
                duration = (24 - startTime) + endTime;
            }
            
            // Format to 2 decimal places
            String formattedDuration = String.format(Locale.getDefault(), "%.2f", duration);
            absenceDurationEditText.setText(formattedDuration);
        }
    }

    private void saveUser() {
        String username = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String role = roleSpinner.getText().toString();

        if (username.isEmpty() || email.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating user...");
        progressDialog.setCancelable(false); // Prevent dismissal while processing
        progressDialog.show();

        checkEmailExists(email, exists -> {
            if (exists) {
                progressDialog.dismiss();
                Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show();
            } else {
                saveUserToFirestore(null, username, email, role, progressDialog);
            }
        });
    }

    private void saveUserToFirestore(String userId, String name, String email, String role, ProgressDialog progressDialog) {
        // Get password from the field or use default
        EditText passwordField = findViewById(R.id.edit_text_password);
        String password = passwordField.getText().toString().trim();
        
        if (password.isEmpty()) {
            password = "password123"; // Default password
        }

        final String finalPassword = password;
        
        // Get admin credentials from AuthContext
        String adminEmail = AuthContext.getInstance().getAdminEmail();
        String adminPassword = AuthContext.getInstance().getAdminPassword();

        if (adminEmail == null || adminPassword == null) {
            progressDialog.dismiss();
            Toast.makeText(AddUserActivity.this, "Admin credentials not found. Please log in again.", Toast.LENGTH_SHORT).show();
            // Redirect to login
            Intent intent = new Intent(AddUserActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        // Re-authenticate admin
        AuthCredential adminCredential = EmailAuthProvider.getCredential(adminEmail, adminPassword);
        mAuth.signInWithCredential(adminCredential)
            .addOnSuccessListener(adminAuthResult -> {
                // Create user document
                Map<String, Object> user = new HashMap<>();
                user.put("name", name);
                user.put("email", email);
                user.put("role", role.toLowerCase().trim());
                user.put("status", "active");
                user.put("createdAt", System.currentTimeMillis());
                user.put("lastUpdated", System.currentTimeMillis());
                user.put("createdBy", mAuth.getCurrentUser().getUid());

                // Add additional info
                Map<String, Object> additionalInfo = new HashMap<>();
                additionalInfo.put("phone", "0");
                user.put("additionalInfo", additionalInfo);

                // Create Authentication user
                mAuth.createUserWithEmailAndPassword(email, finalPassword)
                    .addOnSuccessListener(authResult -> {
                        String newUserId = authResult.getUser().getUid();
                        user.put("uid", newUserId);

                        // Save to Firestore
                        db.collection("users").document(newUserId)
                            .set(user)
                            .addOnSuccessListener(aVoid -> {
                                // Re-authenticate admin after successful user creation
                                mAuth.signInWithCredential(adminCredential)
                                    .addOnSuccessListener(result -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(AddUserActivity.this,
                                            "User created successfully\nEmail: " + email + 
                                            "\nPassword: " + finalPassword,
                                            Toast.LENGTH_LONG).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(AddUserActivity.this,
                                            "Error re-authenticating admin: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    });
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                authResult.getUser().delete();
                                Toast.makeText(AddUserActivity.this,
                                    "Error creating user document: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            });
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(AddUserActivity.this,
                            "Error creating user: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(AddUserActivity.this,
                    "Admin authentication failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void saveAbsence() {
        String teacherName = teacherSpinner.getText().toString();
        String status = statusSpinner.getText().toString();
        String durationStr = absenceDurationEditText.getText().toString().trim();

        // Validate all fields
        if (teacherName.isEmpty() || status.isEmpty() || selectedTimestamp == 0 || 
            startHour == -1 || endHour == -1) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String niveau = absenceNiveauSpinner.getText().toString();
        String licence = absenceLicenceSpinner.getText().toString();
        String groupe = absenceGroupeEditText.getText().toString();

        if (niveau.isEmpty() || licence.isEmpty() || groupe.isEmpty()) {
            Toast.makeText(this, "Please fill in all class fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Format class string
        String classInfo = String.format("%s %s %s", niveau, licence, groupe);

        // Create absence document
        Map<String, Object> absence = new HashMap<>();
        absence.put("teacherName", teacherName);
        absence.put("status", status);
        absence.put("date", selectedTimestamp);
        absence.put("startTime", String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute));
        absence.put("endTime", String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute));
        absence.put("duration", Double.parseDouble(durationStr));
        absence.put("recordedAt", System.currentTimeMillis());
        absence.put("recordedBy", mAuth.getCurrentUser().getUid());
        absence.put("class", classInfo);

        // Save to Firestore
        db.collection("absences")
                .add(absence)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Absence recorded successfully", 
                                 Toast.LENGTH_SHORT).show();
                    clearAbsenceFields();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error recording absence: " + e.getMessage(), 
                                 Toast.LENGTH_LONG).show();
                });
    }

    private void clearAbsenceFields() {
        teacherSpinner.setText("");
        statusSpinner.setText("");
        startTimeEditText.setText("");
        endTimeEditText.setText("");
        absenceDurationEditText.setText("");
        selectedTimestamp = 0;
        dateButton.setText("Select Date");
        startHour = -1;
        startMinute = -1;
        endHour = -1;
        endMinute = -1;
        absenceNiveauSpinner.setText("");
        absenceLicenceSpinner.setText("");
        absenceGroupeEditText.setText("");
    }

    private void setupNiveauSpinner(AutoCompleteTextView niveauSpinner, AutoCompleteTextView licenceSpinner) {
        String[] niveaux = new String[]{"1", "2", "3", "M1", "M2"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, niveaux);
        niveauSpinner.setAdapter(adapter);

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
        licenceSpinner.setText("");
    }

    private void setupClassManagementListeners() {
        saveClassButton.setOnClickListener(v -> saveClass());
        deleteClassButton.setOnClickListener(v -> deleteClass());
    }

    private void saveClass() {
        String niveau = niveauSpinner.getText().toString();
        String licence = licenceSpinner.getText().toString();
        String groupeStr = groupeEditText.getText().toString();

        if (niveau.isEmpty() || licence.isEmpty() || groupeStr.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        int groupe;
        try {
            groupe = Integer.parseInt(groupeStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Numéro de groupe invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> classData = new HashMap<>();
        classData.put("niveau", niveau);
        classData.put("licence", licence);
        classData.put("groupe", groupe);
        classData.put("createdAt", System.currentTimeMillis());
        classData.put("updatedAt", System.currentTimeMillis());

        db.collection("classes")
            .add(classData)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(this, "Classe ajoutée avec succès", Toast.LENGTH_SHORT).show();
                clearClassFields();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Erreur lors de l'ajout de la classe: " + 
                    e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void deleteClass() {
        String niveau = niveauSpinner.getText().toString();
        String licence = licenceSpinner.getText().toString();
        String groupeStr = groupeEditText.getText().toString();

        if (niveau.isEmpty() || licence.isEmpty() || groupeStr.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une classe à supprimer", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        int groupe = Integer.parseInt(groupeStr);

        db.collection("classes")
            .whereEqualTo("niveau", niveau)
            .whereEqualTo("licence", licence)
            .whereEqualTo("groupe", groupe)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    queryDocumentSnapshots.getDocuments().get(0).getReference().delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Classe supprimée avec succès", 
                                Toast.LENGTH_SHORT).show();
                            clearClassFields();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Erreur lors de la suppression: " + 
                                e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                } else {
                    Toast.makeText(this, "Classe non trouvée", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Erreur lors de la recherche: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            });
    }

    private void clearClassFields() {
        niveauSpinner.setText("");
        licenceSpinner.setText("");
        groupeEditText.setText("");
    }

    private void setupTeacherClassSpinners() {
        // Setup niveau spinner
        String[] niveaux = new String[]{"1", "2", "3", "M1", "M2"};
        ArrayAdapter<String> niveauAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, niveaux);
        teacherNiveauSpinner.setAdapter(niveauAdapter);

        teacherNiveauSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedNiveau = teacherNiveauSpinner.getText().toString();
            updateTeacherLicenceOptions(selectedNiveau);
        });
    }

    private void updateTeacherLicenceOptions(String niveau) {
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
        teacherLicenceSpinner.setAdapter(adapter);
        teacherLicenceSpinner.setText("");
    }

    private void setupAbsenceClassSpinners() {
        // Setup niveau spinner
        String[] niveaux = new String[]{"1", "2", "3", "M1", "M2"};
        ArrayAdapter<String> niveauAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, niveaux);
        absenceNiveauSpinner.setAdapter(niveauAdapter);

        absenceNiveauSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedNiveau = absenceNiveauSpinner.getText().toString();
            updateAbsenceLicenceOptions(selectedNiveau);
        });
    }

    private void updateAbsenceLicenceOptions(String niveau) {
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
        absenceLicenceSpinner.setAdapter(adapter);
        absenceLicenceSpinner.setText("");
    }

    private View addNewClassFields() {
        classCount++;
        View classView = getLayoutInflater().inflate(R.layout.teacher_class_item, null);
        
        // Initialize the new class fields
        AutoCompleteTextView niveauSpinner = classView.findViewById(R.id.spinner_teacher_niveau);
        AutoCompleteTextView licenceSpinner = classView.findViewById(R.id.spinner_teacher_licence);
        EditText groupeEditText = classView.findViewById(R.id.edit_text_teacher_groupe);
        
        // Set up the spinners
        setupNiveauSpinner(niveauSpinner, licenceSpinner);
        
        // Add to container
        teacherClassesContainer.addView(classView);
        classViews.add(classView);
        
        return classView;
    }
} 