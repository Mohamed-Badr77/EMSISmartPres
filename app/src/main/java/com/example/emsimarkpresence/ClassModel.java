package com.example.emsimarkpresence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassModel {
    private String id; // Only used for editing existing classes
    private String className;
    private Map<String, Boolean> groupMap; // Map of group IDs to booleans (selected groups)
    private String status;  // "Active", "Paused", "Completed"
    private int numberOfWeeks; // Default 7
    private double hoursPerSession; // 1.5 or 2 hours for example
    private double totalHours; // Calculated from numberOfWeeks * hoursPerSession

    public ClassModel() {
        this.numberOfWeeks = 7; // Default value
        this.hoursPerSession = 1.5; // Default value
        this.status = "Active"; // Default status
    }

    public ClassModel(String className, Map<String, Boolean> groupMap, String status,
                      int numberOfWeeks, double hoursPerSession) {
        this.className = className;
        this.groupMap = groupMap;
        this.status = status;
        this.numberOfWeeks = numberOfWeeks;
        this.hoursPerSession = hoursPerSession;
        this.totalHours = calculateTotalHours();
    }

    // Calculate total hours from numberOfWeeks and hoursPerSession
    private double calculateTotalHours() {
        return numberOfWeeks * hoursPerSession;
    }

    // Update total hours when numberOfWeeks or hoursPerSession changes
    public void updateTotalHours() {
        this.totalHours = calculateTotalHours();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Map<String, Boolean> getGroupMap() { return groupMap; }
    public void setGroupMap(Map<String, Boolean> groupMap) { this.groupMap = groupMap; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getNumberOfWeeks() { return numberOfWeeks; }
    public void setNumberOfWeeks(int numberOfWeeks) {
        this.numberOfWeeks = numberOfWeeks;
        updateTotalHours();
    }

    public double getHoursPerSession() { return hoursPerSession; }
    public void setHoursPerSession(double hoursPerSession) {
        this.hoursPerSession = hoursPerSession;
        updateTotalHours();
    }

    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }

    // Add these methods to your ClassModel class
    public List<String> getSelectedGroupNames() {
        List<String> selectedGroups = new ArrayList<>();
        if (groupMap != null) {
            for (Map.Entry<String, Boolean> entry : groupMap.entrySet()) {
                if (entry.getValue()) {
                    selectedGroups.add(entry.getKey());
                }
            }
        }
        return selectedGroups;
    }

    public void setSelectedGroups(List<String> groupNames) {
        if (groupMap == null) {
            groupMap = new HashMap<>();
        } else {
            groupMap.clear();
        }
        for (String groupName : groupNames) {
            groupMap.put(groupName, true);
        }
    }
}