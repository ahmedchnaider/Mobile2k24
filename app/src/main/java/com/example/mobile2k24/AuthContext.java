package com.example.mobile2k24;

import java.util.HashMap;
import java.util.Map;

public class AuthContext {
    private static AuthContext instance;
    private String userId;
    private String name;
    private String email;
    private String role;
    private Map<String, Object> userData;
    private String adminEmail;
    private String adminPassword;

    private AuthContext() {
        userData = new HashMap<>();
    }

    public static AuthContext getInstance() {
        if (instance == null) {
            instance = instance = new AuthContext();
        }
        return instance;
    }

    public void setUserData(String userId, String role, Map<String, Object> userData) {
        this.userId = userId;
        this.role = role;
        this.userData = userData;
        this.email = userData.get("email") != null ? userData.get("email").toString() : "";
        this.name = userData.get("name") != null ? userData.get("name").toString() : "";
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Map<String, Object> getUserData() { return userData; }
    
    public void setAdminCredentials(String email, String password) {
        this.adminEmail = email;
        this.adminPassword = password;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void clear() {
        userId = null;
        name = null;
        email = null;
        role = null;
        adminEmail = null;
        adminPassword = null;
        userData.clear();
    }
} 