package com.gestiontache.model;

/** Priority level of a task, from most to least urgent. */
public enum Priority {
    HAUTE("Haute"),
    MOYENNE("Moyenne"),
    BASSE("Basse");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
