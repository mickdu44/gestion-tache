package com.gestiontache.controller;

import com.gestiontache.model.Recurrence;
import com.gestiontache.model.SubTask;
import com.gestiontache.model.Task;
import com.gestiontache.model.TaskStatus;
import com.gestiontache.util.DescriptionFormatter;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Builds a compact, read-only preview of a task (title, badges, description,
 * subtasks, attachments) shown in a {@link javafx.scene.control.Tooltip} on
 * hover, so a task can be consulted without opening its editor. Unlike the
 * detail panel/popup, nothing here is interactive (plain labels, no
 * checkboxes or clickable links): a tooltip disappears on its own the
 * moment the mouse moves, so it isn't a place to act on a task.
 */
final class TaskPreview {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);

    private TaskPreview() {
    }

    static Node build(Task task) {
        VBox root = new VBox(8);
        root.setPadding(new Insets(10));
        root.setMaxWidth(300);
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
            TextFlow descriptionFlow = new TextFlow();
            descriptionFlow.getChildren().addAll(DescriptionFormatter.toNodes(description));
            descriptionFlow.getStyleClass().add("task-preview-description");
            root.getChildren().add(descriptionFlow);
        }

        if (!task.getSubtasks().isEmpty()) {
            long done = task.getSubtasks().stream().filter(SubTask::isCompleted).count();
            root.getChildren().add(sectionTitle("Sous-taches (" + done + "/" + task.getSubtasks().size() + ")"));
            VBox subtaskList = new VBox(2);
            for (SubTask subTask : task.getSubtasks()) {
                Label item = new Label((subTask.isCompleted() ? "☑ " : "☐ ") + subTask.getTitle());
                item.setWrapText(true);
                item.getStyleClass().add("task-preview-subtask");
                if (subTask.isCompleted()) {
                    item.getStyleClass().add("task-preview-subtask-completed");
                }
                subtaskList.getChildren().add(item);
            }
            root.getChildren().add(subtaskList);
        }

        if (!task.getAttachments().isEmpty()) {
            root.getChildren().add(sectionTitle("Pieces jointes"));
            VBox attachmentList = new VBox(2);
            for (String path : task.getAttachments()) {
                Label item = new Label(new File(path).getName());
                item.setWrapText(true);
                item.getStyleClass().add("task-preview-attachment");
                attachmentList.getChildren().add(item);
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
}
