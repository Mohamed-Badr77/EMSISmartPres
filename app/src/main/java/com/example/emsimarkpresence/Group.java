package com.example.emsimarkpresence;

import java.util.HashMap;
import java.util.Map;

public class Group {
    private String id;
    private String name;
    private Campus campus;
    private Map<String, Boolean> students = new HashMap<>();
    private Map<String, Boolean> classes = new HashMap<>();
    private Map<String, String> classTeachers = new HashMap<>();

    public Group() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Campus getCampus() {
        return campus;
    }

    public void setCampus(Campus campus) {
        this.campus = campus;
    }

    public Map<String, Boolean> getClasses() {
        return classes;
    }

    public void setClasses(Map<String, Boolean> classes) {
        this.classes = classes;
    }

    public Map<String, Boolean> getStudents() {
        return students;
    }

    public void setStudents(Map<String, Boolean> students) {
        this.students = students;

    }

    public Map<String, String> getClassTeachers() {
        return classTeachers;
    }

    public void setClassTeachers(Map<String, String> classTeachers) {
        this.classTeachers = classTeachers;
    }
}
