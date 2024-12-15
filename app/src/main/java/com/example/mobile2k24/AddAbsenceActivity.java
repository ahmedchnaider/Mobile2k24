package com.example.mobile2k24;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.mobile2k24.adapters.AbsenceHistoryAdapter;

public class AddAbsenceActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 1001;

    private FirebaseFirestore db;
    private AutoCompleteTextView filterTeacherSpinner;
    private Button startDateButton;
    private Button endDateButton;
    private Button generateReportButton;
    private long startDateTimestamp = 0;
    private long endDateTimestamp = 0;
    private RecyclerView absenceHistoryRecyclerView;
    private AbsenceHistoryAdapter absenceHistoryAdapter;
    private List<Map<String, Object>> absencesList = new ArrayList<>();
    private AutoCompleteTextView statsNiveauSpinner;
    private AutoCompleteTextView statsLicenceSpinner;
    private EditText statsGroupeEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_absence);

        try {
            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            
            // Initialize lists
            absencesList = new ArrayList<>();
            
            // Initialize views and setup UI
            initializeViews();
            setupDateRangePickers();
            setupRecyclerView();
            setupStatsClassSpinners();
            
            // Load initial data
            loadAbsenceHistory();
        } catch (Exception e) {
            Log.e("AddAbsenceActivity", "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error initializing activity: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
        
        generateReportButton.setOnClickListener(v -> requestManageStoragePermission());
        debugFirestore();
    }

    private void initializeViews() {
        filterTeacherSpinner = findViewById(R.id.spinner_filter_teacher);
        startDateButton = findViewById(R.id.button_start_date);
        endDateButton = findViewById(R.id.button_end_date);
        generateReportButton = findViewById(R.id.button_generate_report);
        absenceHistoryRecyclerView = findViewById(R.id.recycler_absence_history);
        statsNiveauSpinner = findViewById(R.id.spinner_stats_niveau);
        statsLicenceSpinner = findViewById(R.id.spinner_stats_licence);
        statsGroupeEditText = findViewById(R.id.edit_text_stats_groupe);

        // Initialize teacher spinner with empty list
        List<String> teacherOptions = new ArrayList<>();
        
        ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            teacherOptions
        );
        filterTeacherSpinner.setAdapter(teacherAdapter);

        // Fetch teachers
        fetchTeachers(teacherAdapter);
    }

    private void setupStatsClassSpinners() {
        // Setup niveau spinner
        String[] niveaux = new String[]{"1", "2", "3", "M1", "M2"};
        ArrayAdapter<String> niveauAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, niveaux);
        statsNiveauSpinner.setAdapter(niveauAdapter);

        // Setup niveau change listener
        statsNiveauSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedNiveau = statsNiveauSpinner.getText().toString();
            updateStatsLicenceOptions(selectedNiveau);
        });
    }

    private void setupDateRangePickers() {
        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                if (isStartDate) {
                    startDateTimestamp = selectedDate.getTimeInMillis();
                    startDateButton.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                } else {
                    endDateTimestamp = selectedDate.getTimeInMillis();
                    endDateButton.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void fetchTeachers(ArrayAdapter<String> adapter) {
        db.collection("users")
            .whereEqualTo("role", "teacher")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    String teacherName = document.getString("name");
                    if (teacherName != null) {
                        adapter.add(teacherName);
                    }
                }
                adapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Log.e("AddAbsenceActivity", "Error fetching teachers", e);
                Toast.makeText(this, "Error loading teachers: " + e.getMessage(), 
                             Toast.LENGTH_SHORT).show();
            });
    }

    private void generateReport() {
        if (!validateReportInputs()) {
            return;
        }

        // Fetch the selected filters
        String selectedTeacher = filterTeacherSpinner.getText().toString();
        String niveau = statsNiveauSpinner.getText().toString();
        String licence = statsLicenceSpinner.getText().toString();
        String groupe = statsGroupeEditText.getText().toString();

        // Debug logging
        Log.d("AddAbsenceActivity", "Fetching absences with filters:");
        Log.d("AddAbsenceActivity", "Teacher: " + selectedTeacher);
        Log.d("AddAbsenceActivity", "Niveau: " + niveau);
        Log.d("AddAbsenceActivity", "Licence: " + licence);
        Log.d("AddAbsenceActivity", "Groupe: " + groupe);

        // Fetch all absences and filter in memory
        db.collection("absences")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Map<String, Object>> filteredAbsences = new ArrayList<>();
                
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Map<String, Object> absence = document.getData();
                    if (absence != null && shouldIncludeAbsence(absence, selectedTeacher, niveau, licence, groupe)) {
                        filteredAbsences.add(absence);
                    }
                }

                Log.d("AddAbsenceActivity", "Found " + filteredAbsences.size() + " matching absences");

                if (filteredAbsences.isEmpty()) {
                    Toast.makeText(this, "Aucune absence trouvée pour ces critères", 
                                 Toast.LENGTH_SHORT).show();
                } else {
                    generatePDF(filteredAbsences, selectedTeacher, niveau + licence + groupe);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("AddAbsenceActivity", "Error fetching absences: ", e);
                Toast.makeText(this, "Erreur lors de la génération du rapport: " + e.getMessage(), 
                             Toast.LENGTH_LONG).show();
            });
    }

    private boolean shouldIncludeAbsence(Map<String, Object> absence, 
                                       String selectedTeacher, 
                                       String niveau, 
                                       String licence, 
                                       String groupe) {
        // Debug logging
        Log.d("AddAbsenceActivity", "Filtering absence:");
        Log.d("AddAbsenceActivity", "Selected teacher: '" + selectedTeacher + "'");
        Log.d("AddAbsenceActivity", "Class info: niveau='" + niveau + "', licence='" + 
              licence + "', groupe='" + groupe + "'");

        // If teacher is selected (not empty), check teacher name
        if (!selectedTeacher.isEmpty()) {
            String teacherName = (String) absence.get("teacherName");
            if (!selectedTeacher.equals(teacherName)) {
                return false;
            }
        }

        // If class info is provided, check class
        if (!niveau.isEmpty() && !licence.isEmpty() && !groupe.isEmpty()) {
            String classInfo = String.format("%s %s %s", niveau, licence, groupe);
            String absenceClass = (String) absence.get("class");
            
            Log.d("AddAbsenceActivity", "Class Comparison:");
            Log.d("AddAbsenceActivity", "Expected class: '" + classInfo + "'");
            Log.d("AddAbsenceActivity", "Actual class: '" + absenceClass + "'");
            
            if (absenceClass == null || !absenceClass.equals(classInfo)) {
                return false;
            }
        }

        // Always check date range if provided (this will work for all three scenarios)
        if (startDateTimestamp > 0 && endDateTimestamp > 0) {
            Number dateNum = (Number) absence.get("date");
            if (dateNum == null) {
                return false;
            }
            long date = dateNum.longValue();
            if (date < startDateTimestamp || date > endDateTimestamp) {
                return false;
            }
        }

        // If we get here, the absence matches all provided filters
        return true;
    }

    // Helper method to debug the absence data
    private void debugAbsenceData(Map<String, Object> absence) {
        Log.d("AddAbsenceActivity", "Absence Data:");
        for (Map.Entry<String, Object> entry : absence.entrySet()) {
            Log.d("AddAbsenceActivity", String.format(
                "Field: %s, Value: %s, Type: %s",
                entry.getKey(),
                entry.getValue(),
                entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null"
            ));
        }
    }

    private void generatePDF(List<Map<String, Object>> absences, String teacher, String classInfo) {
        try {
            File directory = new File(getExternalFilesDir(null), "Reports");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = "Rapport_Absences_" + System.currentTimeMillis() + ".pdf";
            File file = new File(directory, fileName);

            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Add title
            Paragraph title = new Paragraph("Rapport des Absences")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            // Add filters applied
            document.add(new Paragraph("\nFiltres appliqués:")
                    .setFontSize(14)
                    .setBold());
            if (!teacher.equals("Enseignant")) {
                document.add(new Paragraph("Enseignant: " + teacher));
            }
            if (!classInfo.isEmpty()) {
                document.add(new Paragraph("Classe: " + classInfo));
            }
            if (startDateTimestamp > 0) {
                document.add(new Paragraph("Date début: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(startDateTimestamp))));
            }
            if (endDateTimestamp > 0) {
                document.add(new Paragraph("Date fin: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(endDateTimestamp))));
            }

            document.add(new Paragraph("\n"));

            // Create table
            Table table = new Table(new float[]{3, 2, 2, 2, 1});
            table.setWidth(UnitValue.createPercentValue(100));

            // Add header cells
            table.addCell(new Cell().add(new Paragraph("Enseignant")));
            table.addCell(new Cell().add(new Paragraph("Classe")));
            table.addCell(new Cell().add(new Paragraph("Date")));
            table.addCell(new Cell().add(new Paragraph("Status")));
            table.addCell(new Cell().add(new Paragraph("Durée")));

            // Add data to table
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            for (Map<String, Object> absence : absences) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(absence.get("teacherName")))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(absence.get("class")))));
                Long dateTimestamp = (Long) absence.get("date");
                table.addCell(new Cell().add(new Paragraph(dateTimestamp != null ? 
                    sdf.format(new Date(dateTimestamp)) : "N/A")));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(absence.get("status")))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(absence.get("duration")))));
            }

            document.add(table);
            document.close();

            Toast.makeText(this, "Rapport généré dans:\n" + file.getAbsolutePath(), 
                         Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, "Error generating PDF: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
            Log.e("AddAbsenceActivity", "IOException in generatePDF: " + e.getMessage());
        }
    }

    private boolean validateReportInputs() {
        if (startDateTimestamp == 0 || endDateTimestamp == 0) {
            Toast.makeText(this, "Veuillez sélectionner une période", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (startDateTimestamp > endDateTimestamp) {
            Toast.makeText(this, "La date de début doit être antérieure à la date de fin", 
                         Toast.LENGTH_SHORT).show();
            return false;
        }

        // Ensure at least one filter is applied (teacher or class)
        String selectedTeacher = filterTeacherSpinner.getText().toString();
        String niveau = statsNiveauSpinner.getText().toString();
        String licence = statsLicenceSpinner.getText().toString();
        String groupe = statsGroupeEditText.getText().toString();
        
        // Log the input values for debugging
        Log.d("AddAbsenceActivity", "Validating inputs:");
        Log.d("AddAbsenceActivity", "Teacher: " + selectedTeacher);
        Log.d("AddAbsenceActivity", "Niveau: " + niveau);
        Log.d("AddAbsenceActivity", "Licence: " + licence);
        Log.d("AddAbsenceActivity", "Groupe: " + groupe);

        boolean isTeacherSelected = !selectedTeacher.equals("Enseignant");
        boolean isClassSelected = !niveau.isEmpty() && !licence.isEmpty() && !groupe.isEmpty();

        if (!isTeacherSelected && !isClassSelected) {
            Toast.makeText(this, "Veuillez sélectionner un enseignant ou une classe", 
                         Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true; // App-specific directory doesn't need permission
        } else {
            return ContextCompat.checkSelfPermission(this, 
                   Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permission granted, proceed with your logic
                generateReport();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        } else {
            // For devices below Android 11, you can request WRITE_EXTERNAL_STORAGE permission
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                    STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateReport(); // Call the report generation if permission is granted
            } else {
                Toast.makeText(this, "Permission de stockage requise pour générer le PDF", 
                             Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupRecyclerView() {
        absenceHistoryAdapter = new AbsenceHistoryAdapter(absencesList, this);
        absenceHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        absenceHistoryRecyclerView.setAdapter(absenceHistoryAdapter);
    }

    private void loadAbsenceHistory() {
        db.collection("absences")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                absencesList.clear();
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    absencesList.add(document.getData());
                }
                absenceHistoryAdapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Log.e("AddAbsenceActivity", "Error loading absences: " + e.getMessage());
                Toast.makeText(this, "Error loading absence history", 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void updateStatsLicenceOptions(String niveau) {
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
        statsLicenceSpinner.setAdapter(adapter);
        statsLicenceSpinner.setText("");
    }

    private void debugFirestore() {
        db.collection("absences")
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                    Log.d("AddAbsenceActivity", "Sample document structure:");
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            Log.d("AddAbsenceActivity", String.format(
                                "Field: %s, Value: %s, Type: %s",
                                entry.getKey(),
                                entry.getValue(),
                                entry.getValue() != null ? 
                                    entry.getValue().getClass().getSimpleName() : "null"
                            ));
                        }
                    }
                } else {
                    Log.d("AddAbsenceActivity", "No documents found in collection");
                }
            })
            .addOnFailureListener(e -> 
                Log.e("AddAbsenceActivity", "Error accessing Firestore: ", e)
            );
    }
} 