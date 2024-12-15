package com.example.mobile2k24;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Date;
import java.text.ParseException;

public class AgentAddAbsenceActivity extends AppCompatActivity {
    private AutoCompleteTextView teacherNameInput;
    private AutoCompleteTextView niveauSpinner;
    private AutoCompleteTextView licenceSpinner;
    private TextInputEditText groupeInput;
    private TextInputEditText durationInput;
    private Button datePickerButton;
    private Button startTimeButton;
    private Button endTimeButton;
    private Button submitButton;
    private Calendar selectedDate;
    private String startTime = "";
    private String endTime = "";
    private FirebaseFirestore db;
    private SimpleDateFormat timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent_add_absence);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // Initialize views
        initializeViews();

        // Set up date picker
        selectedDate = Calendar.getInstance();
        datePickerButton.setOnClickListener(v -> showDatePicker());

        // Set up time pickers
        startTimeButton.setOnClickListener(v -> showTimePicker(true));
        endTimeButton.setOnClickListener(v -> showTimePicker(false));

        // Set up submit button
        submitButton.setOnClickListener(v -> submitAbsence());
    }

    private void initializeViews() {
        teacherNameInput = findViewById(R.id.teacher_name_input);
        niveauSpinner = findViewById(R.id.niveau_spinner);
        licenceSpinner = findViewById(R.id.licence_spinner);
        groupeInput = findViewById(R.id.groupe_input);
        durationInput = findViewById(R.id.duration_input);
        datePickerButton = findViewById(R.id.date_picker_button);
        startTimeButton = findViewById(R.id.start_time_button);
        endTimeButton = findViewById(R.id.end_time_button);
        submitButton = findViewById(R.id.submit_button);

        // Load teachers into spinner
        loadTeachers();

        // Setup class spinners
        setupClassSpinners();

        // Make duration input read-only
        durationInput.setEnabled(false);
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
                teacherNameInput.setAdapter(adapter);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading teachers: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            });
    }

    private void setupClassSpinners() {
        // Setup niveau spinner
        String[] niveaux = new String[]{"1", "2", "3", "M1", "M2"};
        ArrayAdapter<String> niveauAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, niveaux);
        niveauSpinner.setAdapter(niveauAdapter);

        niveauSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedNiveau = niveauSpinner.getText().toString();
            updateLicenceOptions(selectedNiveau);
        });
    }

    private void updateLicenceOptions(String niveau) {
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

    private void calculateDuration() {
        if (startTime.isEmpty() || endTime.isEmpty()) return;

        try {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date startDate = format.parse(startTime);
            Date endDate = format.parse(endTime);
            
            long difference = endDate.getTime() - startDate.getTime();
            if (difference < 0) {
                // If end time is on the next day
                difference += 24 * 60 * 60 * 1000;
            }
            
            float hours = difference / (1000.0f * 60 * 60);
            durationInput.setText(String.format(Locale.getDefault(), "%.2f", hours));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                datePickerButton.setText(String.format("%d/%d/%d", dayOfMonth, month + 1, year));
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker(boolean isStartTime) {
        Calendar currentTime = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                if (isStartTime) {
                    startTime = time;
                    startTimeButton.setText("Start: " + time);
                } else {
                    endTime = time;
                    endTimeButton.setText("End: " + time);
                }
                calculateDuration(); // Calculate duration whenever time changes
            },
            currentTime.get(Calendar.HOUR_OF_DAY),
            currentTime.get(Calendar.MINUTE),
            true
        );
        timePickerDialog.show();
    }

    private void submitAbsence() {
        String teacherName = teacherNameInput.getText().toString().trim();
        String niveau = niveauSpinner.getText().toString().trim();
        String licence = licenceSpinner.getText().toString().trim();
        String groupe = groupeInput.getText().toString().trim();
        String durationStr = durationInput.getText().toString().trim();
        
        // Combine the class information
        String className = String.format("%s %s %s", niveau, licence, groupe);

        if (teacherName.isEmpty() || niveau.isEmpty() || licence.isEmpty() || groupe.isEmpty() 
            || durationStr.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create absence data
        Map<String, Object> absence = new HashMap<>();
        absence.put("teacherName", teacherName);
        absence.put("class", className);
        absence.put("date", selectedDate.getTimeInMillis());
        absence.put("startTime", startTime);
        absence.put("endTime", endTime);
        absence.put("duration", Double.parseDouble(durationStr));
        absence.put("status", "unexcused");
        absence.put("recordedBy", AuthContext.getInstance().getUserId());
        absence.put("recordedAt", Calendar.getInstance().getTimeInMillis());

        // Save to Firestore
        db.collection("absences")
            .add(absence)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(AgentAddAbsenceActivity.this, 
                    "Absence recorded successfully", Toast.LENGTH_SHORT).show();
                finish(); // Return to previous screen
            })
            .addOnFailureListener(e -> {
                Toast.makeText(AgentAddAbsenceActivity.this,
                    "Error recording absence: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }
} 