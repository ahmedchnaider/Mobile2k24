package com.example.mobile2k24;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signupLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // Initialize Firebase Auth and Firestore
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            // Initialize views
            initializeViews();

            loginButton.setOnClickListener(v -> handleLogin());
            signupLink.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(intent);
            });
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onCreate", e);
            Toast.makeText(this, "Error starting app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.username); // Assuming this is actually for email
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);
        signupLink = findViewById(R.id.signup_link);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(email, password)) {
            return;
        }

        progressDialog.show();

        // Authenticate with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Get current user ID
                        String userId = mAuth.getCurrentUser().getUid();
                        // Check user role and redirect accordingly
                        checkUserRoleAndRedirect(userId);
                    } else {
                        progressDialog.dismiss();
                        showError("Login failed: " + task.getException().getMessage());
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return false;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return false;
        }
        return true;
    }

    private void checkUserRoleAndRedirect(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressDialog.dismiss();
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Map<String, Object> userData = documentSnapshot.getData();
                        
                        // Store user data in AuthContext
                        AuthContext.getInstance().setUserData(userId, role, userData);
                        
                        // For admin users, store their credentials securely
                        if ("admin".equals(role)) {
                            // Store admin password securely
                            String email = documentSnapshot.getString("email");
                            String password = passwordEditText.getText().toString();
                            AuthContext.getInstance().setAdminCredentials(email, password);
                        }
                        
                        redirectBasedOnRole(role);
                    } else {
                        showError("User data not found");
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    showError("Error fetching user data: " + e.getMessage());
                });
    }

    private void redirectBasedOnRole(String role) {
        Intent intent = null;
        Log.d("LoginFlow", "Redirecting user with role: " + role);
        
        if (role == null) {
            showError("User role not found");
            return;
        }
        
        // Convert role to lowercase for case-insensitive comparison
        String normalizedRole = role.toLowerCase().trim();
        
        switch (normalizedRole) {
            case "admin":
                intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                Log.d("LoginFlow", "Redirecting to Admin Dashboard");
                break;
            case "teacher":
                intent = new Intent(MainActivity.this, TeacherDashboardActivity.class);
                Log.d("LoginFlow", "Redirecting to Teacher Dashboard");
                break;
            case "agent":
                intent = new Intent(MainActivity.this, AgentDashboardActivity.class);
                Log.d("LoginFlow", "Redirecting to Agent Dashboard");
                break;
            default:
                showError("Invalid user role: " + role);
                return;
        }
        
        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }

    private void showError(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserRoleAndRedirect(currentUser.getUid());
        }
    }
}