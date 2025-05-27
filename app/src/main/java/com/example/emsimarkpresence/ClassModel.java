package com.example.emsimarkpresence;

import java.util.List;
import java.util.Map;

public class ClassModel {
    private String id;
    private String className;
    private int totalHours;
    private List<String> groups; // List of group IDs (deprecated, use groupMap instead)
    private Map<String, Boolean> groupMap; // Map of group IDs to booleans
    private String status;  // "Active", "Paused", "Completed"
    private String teacherId; // ID of the teacher who owns this class

    public ClassModel() {}

    public ClassModel(String className, int totalHours, List<String> groups, String status) {
        this.className = className;
        this.totalHours = totalHours;
        this.groups = groups;
        this.status = status;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public int getTotalHours() { return totalHours; }
    public void setTotalHours(int totalHours) { this.totalHours = totalHours; }
    public List<String> getGroups() { return groups; }
    public void setGroups(List<String> groups) { this.groups = groups; }
    public Map<String, Boolean> getGroupMap() { return groupMap; }
    public void setGroupMap(Map<String, Boolean> groupMap) { this.groupMap = groupMap; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
}