package com.example.emsimarkpresence;

import java.util.List;

public class ClassModel {
    private String className;
    private int totalHours;
    private List<String> groups;
    private String status;  // "Active", "Paused", "Completed"

    // Empty constructor for Firestore
    public ClassModel() {}

    public ClassModel(String className, int totalHours, List<String> groups, String status) {
        this.className = className;
        this.totalHours = totalHours;
        this.groups = groups;
        this.status = status;
    }

    // Getters and setters (required for Firestore)
    public String getClassName() { return className; }
    public int getTotalHours() { return totalHours; }
    public List<String> getGroups() { return groups; }
    public String getStatus() { return status; }
}