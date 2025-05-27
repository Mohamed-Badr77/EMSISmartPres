package com.example.emsimarkpresence;

public enum Campus {
    CENTRE1("Centre 1"),
    CENTRE2("Centre 2"),
    ROUDANI("Roudani"),
    MAARIF("Maarif"),
    ORANGERS("Les Orangers");

    private final String displayName;

    Campus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}