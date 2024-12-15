package com.example.mobile2k24.models;

public class User {
    private String uid;
    private String username;
    private String email;
    private String role;
    private String status;
    private long createdAt;

    // Empty constructor required for Firestore
    public User() {}

    public User(String uid, String username, String email, String role, String status, long createdAt) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
} 