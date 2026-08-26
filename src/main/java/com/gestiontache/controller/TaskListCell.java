package com.gestiontache.controller;

import com.gestiontache.model.Task;
import com.gestiontache.util.DescriptionFormatter;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Renders a single task: a checkbox to mark it done, its title/description,
 * a priority badge, the date it belongs to (shown only while browsing
 * search results) and edit/delete actions. When browsing a single day
 * without any priority filter active, rows can be dragged to reorder the
 * tasks manually.
 */
public class TaskListCell extends ListCell<Task> {

    private static final DateTimeFormatter DATE_BADGE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);

    private final Label dragHandle = new Label("≡");
    private final CheckBox doneCheckBox = new CheckBox();
    private final Label titleLabel = new Label();
    private final Label descriptionLabel = new Label();
    private final Label priorityBadge = new Label();
    private final Label dateBadge = new Label();
    private final Button editButton = new Button("Modifier");
    private final Button deleteButton = new Button("Supprimer");
    private final HBox root;

    private final BiConsumer<Task, Boolean> onToggle;
    private final Consumer<Task> onEdit;
    private final Consumer<Task> onDelete;
    private final boolean showDateBadge;
    private final boolean reorderEnabled;
    private final Runnable onReorder;

    public TaskListCell(BiConsumer<Task, Boolean> onToggle, Consumer<Task> onEdit,
                         Consumer<Task> onDelete, boolean showDateBadge, boolean reorderEnabled,
                         Runnable onReorder) {
        this.onToggle = onToggle;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.showDateBadge = showDateBadge;
        this.reorderEnabled = reorderEnabled;
        this.onReorder = onReorder;

        dragHandle.getStyleClass().add("drag-handle");
        titleLabel.getStyleClass().add("task-title");
        descriptionLabel.getStyleClass().add("task-description");
        descriptionLabel.setWrapText(true);
        priorityBadge.getStyleClass().add("priority-badge");
        dateBadge.getStyleClass().add("task-date-badge");
        editButton.getStyleClass().add("icon-button");
        deleteButton.getStyleClass().add("icon-button");

        VBox textBox = new VBox(2, titleLabel, descriptionLabel);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        root = new HBox(10, dragHandle, doneCheckBox, textBox, priorityBadge, dateBadge, editButton, deleteButton);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(8, 10, 8, 10));
        root.getStyleClass().add("task-row");

        dragHandle.setVisible(reorderEnabled);
        dragHandle.setManaged(reorderEnabled);

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

        if (reorderEnabled) {
            setupDragAndDrop();
        }
    }

    private void setupDragAndDrop() {
        root.setOnDragDetected(event -> {
            if (getItem() == null) {
                return;
            }
            Dragboard dragboard = root.startDragAndDrop(TransferMode.MOVE);
            dragboard.setDragView(root.snapshot(null, null));
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(getIndex()));
            dragboard.setContent(content);
            event.consume();
        });

        root.setOnDragOver(event -> {
            if (event.getGestureSource() != root && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        root.setOnDragEntered(event -> {
            if (event.getGestureSource() != root && event.getDragboard().hasString()) {
                root.getStyleClass().add("task-row-drag-over");
            }
        });

        root.setOnDragExited(event -> root.getStyleClass().remove("task-row-drag-over"));

        root.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasString()) {
                int draggedIndex = Integer.parseInt(dragboard.getString());
                int targetIndex = getIndex();
                ObservableList<Task> items = getListView().getItems();
                if (draggedIndex >= 0 && draggedIndex < items.size()
                        && targetIndex >= 0 && targetIndex < items.size()
                        && draggedIndex != targetIndex) {
                    Task dragged = items.remove(draggedIndex);
                    int insertIndex = targetIndex > draggedIndex ? targetIndex - 1 : targetIndex;
                    items.add(insertIndex, dragged);
                    getListView().getSelectionModel().select(dragged);
                    success = true;
                    if (onReorder != null) {
                        onReorder.run();
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        root.setOnDragDone(event -> {
            root.getStyleClass().remove("task-row-drag-over");
            event.consume();
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
        boolean hasDescription = description != null && !description.isBlank();
        if (hasDescription) {
            String plain = DescriptionFormatter.toPlainText(description);
            int newline = plain.indexOf('\n');
            descriptionLabel.setText(newline >= 0 ? plain.substring(0, newline) + " …" : plain);
        } else {
            descriptionLabel.setText("");
        }
        descriptionLabel.setManaged(hasDescription);
        descriptionLabel.setVisible(hasDescription);

        priorityBadge.setText(task.getPriority().toString());
        priorityBadge.getStyleClass().removeIf(c -> c.startsWith("priority-") && !c.equals("priority-badge"));
        priorityBadge.getStyleClass().add("priority-" + task.getPriority().name().toLowerCase(Locale.ROOT));

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
