package com.gestiontache.controller;

import com.gestiontache.model.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

/** Renders one archived task with its date/priority and restore/delete actions. */
public class ArchiveListCell extends ListCell<Task> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);

    private final Label titleLabel = new Label();
    private final Label dateBadge = new Label();
    private final Label priorityBadge = new Label();
    private final Button restoreButton = new Button("Restaurer");
    private final Button deleteButton = new Button("Supprimer definitivement");
    private final HBox root;

    public ArchiveListCell(Consumer<Task> onRestore, Consumer<Task> onDelete) {
        titleLabel.getStyleClass().add("task-title-completed");
        dateBadge.getStyleClass().add("task-date-badge");
        priorityBadge.getStyleClass().add("priority-badge");
        restoreButton.getStyleClass().add("icon-button");
        deleteButton.getStyleClass().add("icon-button");

        VBox textBox = new VBox(titleLabel);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        root = new HBox(10, textBox, priorityBadge, dateBadge, restoreButton, deleteButton);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(8, 10, 8, 10));
        root.getStyleClass().add("task-row");

        restoreButton.setOnAction(e -> {
            Task task = getItem();
            if (task != null) {
                onRestore.accept(task);
            }
        });
        deleteButton.setOnAction(e -> {
            Task task = getItem();
            if (task != null) {
                onDelete.accept(task);
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

        titleLabel.setText(task.getTitle());
        dateBadge.setText(task.getDate().format(DATE_FORMAT));

        priorityBadge.setText(task.getPriority().toString());
        priorityBadge.getStyleClass().removeIf(c -> c.startsWith("priority-") && !c.equals("priority-badge"));
        priorityBadge.getStyleClass().add("priority-" + task.getPriority().name().toLowerCase(Locale.ROOT));

        setGraphic(root);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }
}
