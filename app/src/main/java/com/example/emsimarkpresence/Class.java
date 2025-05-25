package com.example.emsimarkpresence;

import java.util.HashMap;
import java.util.Map;

public class Class {
    private String id;
    private String name;
    private Map<String, Boolean> groups = new HashMap<>();

    public Class() {
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

    public Map<String, Boolean> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Boolean> groups) {
        this.groups = groups;
    }
}
