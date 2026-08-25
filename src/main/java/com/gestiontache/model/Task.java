package com.gestiontache.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single work task attached to a given day.
 */
public class Task {

    private String id;
    private String title;
    private String description;
    private LocalDate date;
    private boolean completed;
    private LocalDateTime createdAt;
    private int order;
    private Priority priority = Priority.MOYENNE;
    private Recurrence recurrence = Recurrence.AUCUNE;

    public Task() {
        // Required for JSON deserialization.
    }

    public Task(String title, String description, LocalDate date) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.date = date;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
        this.order = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Manual rank among the tasks of the same day, used for drag-and-drop ordering. */
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    /**
     * A task matches a keyword when the keyword (case-insensitive) appears
     * either in the title or in the description.
     */
    public boolean matches(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lower = keyword.trim().toLowerCase();
        boolean inTitle = title != null && title.toLowerCase().contains(lower);
        boolean inDescription = description != null && description.toLowerCase().contains(lower);
        return inTitle || inDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
