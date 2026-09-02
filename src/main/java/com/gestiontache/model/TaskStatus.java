package com.gestiontache.model;

/** Workflow status of a task, from not started to done. */
public enum TaskStatus {
    A_FAIRE("A faire"),
    EN_COURS("En cours"),
    TERMINEE("Terminee");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
