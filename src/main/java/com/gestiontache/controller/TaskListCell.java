package com.gestiontache.controller;

import com.gestiontache.model.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Renders a single task: a checkbox to mark it done, its title/description,
 * the date it belongs to (shown only while browsing search results) and
 * edit/delete actions.
 */
public class TaskListCell extends ListCell<Task> {

    private static final DateTimeFormatter DATE_BADGE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);

    private final CheckBox doneCheckBox = new CheckBox();
    private final Label titleLabel = new Label();
    private final Label descriptionLabel = new Label();
    private final Label dateBadge = new Label();
    private final Button editButton = new Button("Modifier");
    private final Button deleteButton = new Button("Supprimer");
    private final HBox root;

    private final BiConsumer<Task, Boolean> onToggle;
    private final Consumer<Task> onEdit;
    private final Consumer<Task> onDelete;
    private final boolean showDateBadge;

    public TaskListCell(BiConsumer<Task, Boolean> onToggle, Consumer<Task> onEdit,
                         Consumer<Task> onDelete, boolean showDateBadge) {
        this.onToggle = onToggle;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.showDateBadge = showDateBadge;

        titleLabel.getStyleClass().add("task-title");
        descriptionLabel.getStyleClass().add("task-description");
        descriptionLabel.setWrapText(true);
        dateBadge.getStyleClass().add("task-date-badge");
        editButton.getStyleClass().add("icon-button");
        deleteButton.getStyleClass().add("icon-button");

        VBox textBox = new VBox(2, titleLabel, descriptionLabel);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        root = new HBox(10, doneCheckBox, textBox, dateBadge, editButton, deleteButton);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(8, 10, 8, 10));
        root.getStyleClass().add("task-row");

        doneCheckBox.setOnAction(e -> {
            Task task = getItem();
            if (task != null) {
                this.onToggle.accept(task, doneCheckBox.isSelected());
            }
        });
        editButton.setOnAction(e -> {
            Task task = getItem();
            if (task != null) {
                this.onEdit.accept(task);
            }
        });
        deleteButton.setOnAction(e -> {
            Task task = getItem();
            if (task != null) {
                this.onDelete.accept(task);
            }
        });
    }

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        if (empty || task == null) {
            setGraphic(null);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return;
        }

        doneCheckBox.setSelected(task.isCompleted());
        titleLabel.setText(task.getTitle());
        titleLabel.getStyleClass().removeAll("task-title-completed");
        if (task.isCompleted()) {
            titleLabel.getStyleClass().add("task-title-completed");
        }

        String description = task.getDescription();
        descriptionLabel.setText(description == null || description.isBlank() ? "" : description);
        descriptionLabel.setManaged(description != null && !description.isBlank());
        descriptionLabel.setVisible(description != null && !description.isBlank());

        if (showDateBadge && task.getDate() != null) {
            dateBadge.setText(task.getDate().format(DATE_BADGE_FORMAT));
            dateBadge.setVisible(true);
            dateBadge.setManaged(true);
        } else {
            dateBadge.setVisible(false);
            dateBadge.setManaged(false);
        }

        setGraphic(root);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }
}
