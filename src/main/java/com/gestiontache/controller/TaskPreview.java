package com.gestiontache.controller;

import com.gestiontache.model.Recurrence;
import com.gestiontache.model.SubTask;
import com.gestiontache.model.Task;
import com.gestiontache.model.TaskStatus;
import com.gestiontache.util.DescriptionFormatter;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Builds a compact, read-only preview of a task (title, badges, description,
 * subtasks, attachments) shown in a {@link javafx.scene.control.Tooltip} on
 * hover, so a task can be consulted without opening its editor. Unlike the
 * detail panel/popup, nothing here is interactive (plain labels, no
 * checkboxes or clickable links): a tooltip disappears on its own the
 * moment the mouse moves, so it isn't a place to act on a task.
 * <p>
 * The caller ({@link TaskListCell}/{@link KanbanTaskCell}) sizes the
 * popup itself (a fixed fraction of the main window) and wraps this node
 * in a scroll pane. The description, subtask list and attachment list
 * are still each capped (character count or item count) on top of that,
 * since a tooltip is transient — it closes on the next mouse move, before
 * a user could scroll it — so an overlong preview must be trimmed instead
 * of letting it grow the popup past a readable height.
 */
final class TaskPreview {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);
    private static final int MAX_DESCRIPTION_CHARS = 240;
    private static final int MAX_SUBTASKS_SHOWN = 6;
    private static final int MAX_ATTACHMENTS_SHOWN = 4;

    /** Fraction of the main window's size the popup is sized to. */
    private static final double WIDTH_FRACTION = 1.0 / 3.0;
    private static final double HEIGHT_FRACTION = 1.0 / 2.0;
    /** Used before the cell is attached to a Scene, or the window size is otherwise unknown. */
    private static final double FALLBACK_WIDTH = 420;
    private static final double FALLBACK_HEIGHT = 400;
    /** Keeps the popup usable even against a very small window. */
    private static final double MIN_WIDTH = 280;
    private static final double MIN_HEIGHT = 200;

    private TaskPreview() {
    }

    /**
     * Sizes {@code tooltip} and the {@code scrollPane} holding its content to
     * one third of {@code anchor}'s window width and half its height, so the
     * popup always matches the main window's proportions rather than
     * shrinking or growing with the previewed task's content. Called from
     * the tooltip's {@code onShowing} handler (not once at construction time)
     * so a resized window is picked up on the next hover.
     */
    static void sizeToWindow(Tooltip tooltip, ScrollPane scrollPane, Node anchor) {
        Window window = anchor.getScene() != null ? anchor.getScene().getWindow() : null;
        double width = window != null
                ? Math.max(MIN_WIDTH, window.getWidth() * WIDTH_FRACTION)
                : FALLBACK_WIDTH;
        double height = window != null
                ? Math.max(MIN_HEIGHT, window.getHeight() * HEIGHT_FRACTION)
                : FALLBACK_HEIGHT;
        tooltip.setPrefWidth(width);
        tooltip.setPrefHeight(height);
        scrollPane.setPrefWidth(width);
        scrollPane.setPrefHeight(height);
    }

    static Node build(Task task) {
        VBox root = new VBox(8);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("task-preview");

        Label title = new Label(task.getTitle());
        title.getStyleClass().add("task-preview-title");
        title.setWrapText(true);
        root.getChildren().add(title);

        FlowPane badgeRow = new FlowPane(6, 4);
        Label priorityBadge = new Label(task.getPriority().toString());
        priorityBadge.getStyleClass().addAll("priority-badge",
                "priority-" + task.getPriority().name().toLowerCase(Locale.ROOT));
        badgeRow.getChildren().add(priorityBadge);
        if (task.getRecurrence() != Recurrence.AUCUNE) {
            Label recurrenceBadge = new Label("🔁 " + task.getRecurrence());
            recurrenceBadge.getStyleClass().add("recurrence-badge");
            badgeRow.getChildren().add(recurrenceBadge);
        }
        if (task.getStatus() == TaskStatus.EN_COURS) {
            Label statusBadge = new Label("⏳ " + TaskStatus.EN_COURS);
            statusBadge.getStyleClass().add("status-in-progress-badge");
            badgeRow.getChildren().add(statusBadge);
        }
        if (task.getDate() != null) {
            Label dateBadge = new Label(task.getDate().format(DATE_FORMAT));
            dateBadge.getStyleClass().add("task-date-badge");
            badgeRow.getChildren().add(dateBadge);
        }
        root.getChildren().add(badgeRow);

        String description = task.getDescription();
        if (description != null && !description.isBlank()) {
            String preview = description.length() > MAX_DESCRIPTION_CHARS
                    ? description.substring(0, MAX_DESCRIPTION_CHARS) + "…"
                    : description;
            TextFlow descriptionFlow = new TextFlow();
            descriptionFlow.getChildren().addAll(DescriptionFormatter.toNodes(preview));
            descriptionFlow.getStyleClass().add("task-preview-description");
            root.getChildren().add(descriptionFlow);
        }

        List<SubTask> subtasks = task.getSubtasks();
        if (!subtasks.isEmpty()) {
            long done = subtasks.stream().filter(SubTask::isCompleted).count();
            root.getChildren().add(sectionTitle("Sous-taches (" + done + "/" + subtasks.size() + ")"));
            VBox subtaskList = new VBox(2);
            int shown = Math.min(subtasks.size(), MAX_SUBTASKS_SHOWN);
            for (int i = 0; i < shown; i++) {
                SubTask subTask = subtasks.get(i);
                Label item = new Label((subTask.isCompleted() ? "☑ " : "☐ ") + subTask.getTitle());
                item.setWrapText(true);
                item.getStyleClass().add("task-preview-subtask");
                if (subTask.isCompleted()) {
                    item.getStyleClass().add("task-preview-subtask-completed");
                }
                subtaskList.getChildren().add(item);
            }
            if (subtasks.size() > shown) {
                subtaskList.getChildren().add(moreLabel(subtasks.size() - shown));
            }
            root.getChildren().add(subtaskList);
        }

        List<String> attachments = task.getAttachments();
        if (!attachments.isEmpty()) {
            root.getChildren().add(sectionTitle("Pieces jointes"));
            VBox attachmentList = new VBox(2);
            int shown = Math.min(attachments.size(), MAX_ATTACHMENTS_SHOWN);
            for (int i = 0; i < shown; i++) {
                Label item = new Label(new File(attachments.get(i)).getName());
                item.setWrapText(true);
                item.getStyleClass().add("task-preview-attachment");
                attachmentList.getChildren().add(item);
            }
            if (attachments.size() > shown) {
                attachmentList.getChildren().add(moreLabel(attachments.size() - shown));
            }
            root.getChildren().add(attachmentList);
        }

        return root;
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("detail-section-title");
        return label;
    }

    private static Label moreLabel(int remaining) {
        Label label = new Label("+ " + remaining + " autre(s)");
        label.getStyleClass().add("task-preview-more");
        return label;
    }
}
