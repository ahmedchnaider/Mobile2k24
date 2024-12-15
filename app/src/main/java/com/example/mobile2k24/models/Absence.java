package com.example.mobile2k24.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Absence {
    private String id;
    private String className;
    private long recordedAt;
    private String startTime;
    private String endTime;
    private int duration;
    private String status;

    public Absence() {}

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public long getRecordedAt() { return recordedAt; }
    public void setRecordedAt(long recordedAt) { this.recordedAt = recordedAt; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return className + " - " + dateFormat.format(new Date(recordedAt));
    }
} 