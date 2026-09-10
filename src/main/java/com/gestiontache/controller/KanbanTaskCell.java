package com.gestiontache.controller;

import com.gestiontache.model.Recurrence;
import com.gestiontache.model.SubTask;
import com.gestiontache.model.Task;
import com.gestiontache.util.DescriptionFormatter;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Post-it-styled card for a single Kanban column: a checkbox/title/edit row,
 * a truncated description preview, up to 5 directly-checkable subtasks, and
 * a row of badges aligned to the right. Unlike {@link TaskListCell} (a wide
 * row meant for the full-width flat list), this fits a narrow column and
 * looks like a sticky note rather than a plain list row, so its content is
 * always visible instead of needing a hover preview or opening the editor.
 * There is no drag handle, delete button, date badge, or status badge: it
 * is always scoped to a single day and the column a card sits in already
 * says its status; deleting a task is still available from the Jour/Semaine
 * views. Selecting or clicking a card no longer opens its detail popup by
 * itself: only the edit button does, so dragging a card doesn't
 * accidentally pop it open. The whole card is still a drag source (see
 * {@link MainController}'s per-column drop targets), letting a card be
 * dragged into another column to change its status; there is no manual
 * ordering within a column.
 */
public class KanbanTaskCell extends ListCell<Task> {

    private static final int MAX_DESCRIPTION_CHARS = 100;
    private static final int MAX_SUBTASKS_SHOWN = 5;

    private final CheckBox doneCheckBox = new CheckBox();
    private final Label titleLabel = new Label();
    private final Button editButton = new Button("✎");
    private final Label descriptionLabel = new Label();
    private final VBox subtasksBox = new VBox(2);
    private final Label priorityBadge = new Label();
    private final Label recurrenceBadge = new Label();
    private final Label subtaskBadge = new Label();
    private final VBox root;

    private final BiConsumer<Task, Boolean> onToggle;
    private final Consumer<Task> onEdit;
    private final SubtaskToggleHandler onToggleSubtask;

    public KanbanTaskCell(BiConsumer<Task, Boolean> onToggle, Consumer<Task> onEdit,
                           SubtaskToggleHandler onToggleSubtask) {
        this.onToggle = onToggle;
        this.onEdit = onEdit;
        this.onToggleSubtask = onToggleSubtask;

        titleLabel.getStyleClass().add("task-title");
        titleLabel.setWrapText(true);
        titleLabel.setMinWidth(0);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        editButton.getStyleClass().add("icon-button");
        descriptionLabel.getStyleClass().add("postit-description");
        descriptionLabel.setWrapText(true);
        priorityBadge.getStyleClass().add("priority-badge");
        recurrenceBadge.getStyleClass().add("recurrence-badge");
        subtaskBadge.getStyleClass().add("subtask-count-badge");

        HBox topRow = new HBox(8, doneCheckBox, titleLabel, editButton);
        topRow.setAlignment(Pos.CENTER_LEFT);

        FlowPane badgeRow = new FlowPane(6, 4, priorityBadge, recurrenceBadge, subtaskBadge);
        badgeRow.setAlignment(Pos.CENTER_RIGHT);

        root = new VBox(6, topRow, descriptionLabel, subtasksBox, badgeRow);
        root.getStyleClass().add("postit-card");
        root.setPadding(new Insets(10, 12, 10, 12));
        root.setMaxWidth(Double.MAX_VALUE);
        setMaxWidth(Double.MAX_VALUE);

        doneCheckBox.setOnAction(e -> {
            Task task = getItem();
            if (task != null) {
                this.onToggle.accept(task, doneCheckBox.isSelected());
            }
        });
        editButton.setOnAction(e -> {
            Task task = getItem();
            if (task != null) {
                getListView().getSelectionModel().select(task);
                this.onEdit.accept(task);
            }
        });

        root.setOnDragDetected(event -> {
            Task task = getItem();
            if (task == null) {
                return;
            }
            Dragboard dragboard = root.startDragAndDrop(TransferMode.MOVE);
            dragboard.setDragView(root.snapshot(null, null));
            ClipboardContent content = new ClipboardContent();
            content.putString(task.getId());
            dragboard.setContent(content);
            event.consume();
        });
        root.setOnDragDone(event -> event.consume());
    }

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        setPrefWidth(0);
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
            descriptionLabel.setText(plain.length() > MAX_DESCRIPTION_CHARS
                    ? plain.substring(0, MAX_DESCRIPTION_CHARS) + "…"
                    : plain);
        }
        descriptionLabel.setVisible(hasDescription);
        descriptionLabel.setManaged(hasDescription);

        List<SubTask> subtasks = task.getSubtasks();
        boolean hasSubtasks = !subtasks.isEmpty();
        subtasksBox.getChildren().clear();
        if (hasSubtasks) {
            int shown = Math.min(subtasks.size(), MAX_SUBTASKS_SHOWN);
            for (int i = 0; i < shown; i++) {
                SubTask subTask = subtasks.get(i);
                CheckBox subtaskCheckBox = new CheckBox(subTask.getTitle());
                subtaskCheckBox.setSelected(subTask.isCompleted());
                subtaskCheckBox.setWrapText(true);
                subtaskCheckBox.getStyleClass().add("postit-subtask");
                subtaskCheckBox.setOnAction(e -> {
                    Task currentTask = getItem();
                    if (currentTask != null) {
                        onToggleSubtask.toggle(currentTask, subTask, subtaskCheckBox.isSelected());
                    }
                });
                subtasksBox.getChildren().add(subtaskCheckBox);
            }
            if (subtasks.size() > shown) {
                Label more = new Label("+ " + (subtasks.size() - shown) + " autre(s)");
                more.getStyleClass().add("postit-more");
                subtasksBox.getChildren().add(more);
            }
        }
        subtasksBox.setVisible(hasSubtasks);
        subtasksBox.setManaged(hasSubtasks);

        priorityBadge.setText(task.getPriority().toString());
        priorityBadge.getStyleClass().removeIf(c -> c.startsWith("priority-") && !c.equals("priority-badge"));
        priorityBadge.getStyleClass().add("priority-" + task.getPriority().name().toLowerCase(Locale.ROOT));

        boolean recurring = task.getRecurrence() != Recurrence.AUCUNE;
        recurrenceBadge.setText("🔁 " + task.getRecurrence());
        recurrenceBadge.setVisible(recurring);
        recurrenceBadge.setManaged(recurring);

        if (hasSubtasks) {
            long done = subtasks.stream().filter(SubTask::isCompleted).count();
            subtaskBadge.setText(done + "/" + subtasks.size());
        }
        subtaskBadge.setVisible(hasSubtasks);
        subtaskBadge.setManaged(hasSubtasks);

        setGraphic(root);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }
}
