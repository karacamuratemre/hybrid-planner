package com.hybridplanner.model;

public enum WorkDay {
    PAZARTESI("Pazartesi", "Pzt"),
    SALI("Salı", "Sal"),
    CARSAMBA("Çarşamba", "Çar"),
    PERSEMBE("Perşembe", "Per"),
    CUMA("Cuma", "Cum");

    private final String displayName;
    private final String shortName;

    WorkDay(String displayName, String shortName) {
        this.displayName = displayName;
        this.shortName = shortName;
    }

    public String getDisplayName() { return displayName; }
    public String getShortName() { return shortName; }
}
