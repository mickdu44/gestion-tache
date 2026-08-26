package com.gestiontache.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** How often a task repeats once it is marked as completed. */
public enum Recurrence {
    AUCUNE("Aucune"),
    QUOTIDIENNE("Quotidienne"),
    HEBDOMADAIRE("Hebdomadaire"),
    JOURS_OUVRES("Jours ouvres");

    private final String label;

    Recurrence(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Date of the next occurrence after {@code from}, following this
     * recurrence rule. Not applicable to {@link #AUCUNE}.
     */
    public LocalDate nextOccurrence(LocalDate from) {
        return switch (this) {
            case QUOTIDIENNE -> from.plusDays(1);
            case HEBDOMADAIRE -> from.plusWeeks(1);
            case JOURS_OUVRES -> nextBusinessDay(from);
            case AUCUNE -> throw new IllegalStateException("AUCUNE n'a pas d'occurrence suivante");
        };
    }

    private static LocalDate nextBusinessDay(LocalDate from) {
        LocalDate next = from.plusDays(1);
        while (next.getDayOfWeek() == DayOfWeek.SATURDAY || next.getDayOfWeek() == DayOfWeek.SUNDAY) {
            next = next.plusDays(1);
        }
        return next;
    }
}
