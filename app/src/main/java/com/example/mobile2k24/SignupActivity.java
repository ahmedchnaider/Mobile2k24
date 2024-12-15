package com.example.mobile2k24;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile2k24.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.app.ProgressDialog;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    private EditText usernameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button signupButton;
    private TextView backToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Verify Firestore initialization
        if (db == null) {
            Log.e("SignupActivity", "Firestore not initialized!");
            Toast.makeText(this, "Database initialization failed", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Initialize views
        initializeViews();
        
        signupButton.setOnClickListener(v -> handleSignup());
        backToLogin.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        usernameEditText = findViewById(R.id.signup_username);
        emailEditText = findViewById(R.id.signup_email);
        passwordEditText = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);
        backToLogin = findViewById(R.id.back_to_login);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
    }

    private void handleSignup() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(username, email, password)) {
            return;
        }

        progressDialog.show();

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Get the newly created user's ID
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(userId, username, email);
                    } else {
                        progressDialog.dismiss();
                        showError("Registration failed: " + task.getException().getMessage());
                    }
                });
    }

    private boolean validateInputs(String username, String email, String password) {
        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            return false;
        }
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return false;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return false;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    private void saveUserToFirestore(String userId, String username, String email) {
        Log.d("SignupActivity", "Attempting to save user: " + userId);

        Map<String, Object> user = new HashMap<>();
        user.put("uid", userId);
        user.put("name", username);
        user.put("email", email);
        user.put("role", "teacher");
        user.put("status", "active");
        user.put("createdAt", System.currentTimeMillis());
        user.put("lastUpdated", System.currentTimeMillis());
        user.put("createdBy", "user");  // Indicates self-registration

        // Create additionalInfo map
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("phone", "0");
        additionalInfo.put("class", "none");
        
        // Add additionalInfo to user map
        user.put("additionalInfo", additionalInfo);

        db.collection("users")
            .document(userId)
            .set(user)
            .addOnSuccessListener(aVoid -> {
                Log.d("SignupActivity", "User successfully saved to Firestore");
                progressDialog.dismiss();
                Toast.makeText(SignupActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignupActivity.this, MainActivity.class));
                finish();
            })
            .addOnFailureListener(e -> {
                Log.e("SignupActivity", "Error saving user to Firestore", e);
                progressDialog.dismiss();
                showError("Failed to save user data: " + e.getMessage());
            });
    }

    private void showError(String message) {
        Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
    }
} 