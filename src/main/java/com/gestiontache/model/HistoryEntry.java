package com.gestiontache.model;

import java.time.LocalDateTime;

/** A single dated entry in a {@link Task}'s change history. */
public class HistoryEntry {

    private LocalDateTime timestamp;
    private String message;

    public HistoryEntry() {
        // Required for JSON deserialization.
    }

    public HistoryEntry(LocalDateTime timestamp, String message) {
        this.timestamp = timestamp;
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
