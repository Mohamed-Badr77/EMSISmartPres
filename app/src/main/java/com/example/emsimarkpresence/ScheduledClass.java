package com.example.emsimarkpresence;

import java.util.List;

public class ScheduledClass {
    private String id;
    private String classId;
    private String className;
    private String slotId;
    private List<String> groupNames;

    public ScheduledClass() {
        // Required empty constructor for Firebase
    }

    public ScheduledClass(String classId, String className, String slotId, List<String> groupNames) {
        this.classId = classId;
        this.className = className;
        this.slotId = slotId;
        this.groupNames = groupNames;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public List<String> getGroupNames() { return groupNames; }
    public void setGroupNames(List<String> groupNames) { this.groupNames = groupNames; }

    public String getFormattedGroupNames() {
        if (groupNames == null || groupNames.isEmpty()) {
            return "";
        }
        return String.join(", ", groupNames);
    }
}
