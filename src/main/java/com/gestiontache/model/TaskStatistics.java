package com.gestiontache.model;

import java.time.LocalDate;
import java.util.Map;

/**
 * Simple completion statistics: overall totals plus a day-by-day count of
 * tasks completed, keyed by the task's own date (there is no separate
 * "completed at" timestamp in the model). {@code completedPerDay} is
 * ordered chronologically, oldest first.
 */
public record TaskStatistics(int totalTasks, int completedTasks, Map<LocalDate, Long> completedPerDay) {

    public double completionRate() {
        return totalTasks == 0 ? 0.0 : (double) completedTasks / totalTasks;
    }
}
