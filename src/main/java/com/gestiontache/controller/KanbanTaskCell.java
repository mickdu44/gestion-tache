package com.gestiontache.controller;

import com.gestiontache.model.Recurrence;
import com.gestiontache.model.SubTask;
import com.gestiontache.model.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Compact card for a single Kanban column: a checkbox/title/edit row, and a
 * row of badges below it aligned to the right. Unlike {@link TaskListCell}
 * (a wide row meant for the full-width flat list), this fits a narrow
 * column. There is no drag handle, delete button, date badge, or status
 * badge: it is always scoped to a single day and the column a card sits in
 * already says its status; deleting a task is still available from the
 * Jour/Semaine views. Selecting or clicking a card no longer opens its
 * detail popup by itself: only the edit button does, so dragging a card
 * doesn't accidentally pop it open. Hovering the card shows a read-only
 * preview of its detail in a tooltip (see {@link TaskPreview}), useful here
 * since the card itself is too narrow to show much. The whole card is
 * still a drag source (see {@link MainController}'s per-column drop
 * targets), letting a card be dragged into another column to change its
 * status; there is no manual ordering within a column.
 */
public class KanbanTaskCell extends ListCell<Task> {

    private final CheckBox doneCheckBox = new CheckBox();
    private final Label titleLabel = new Label();
    private final Button editButton = new Button("✎");
    private final Label priorityBadge = new Label();
    private final Label recurrenceBadge = new Label();
    private final Label subtaskBadge = new Label();
    private final Tooltip detailTooltip = new Tooltip();
    private final VBox root;

    private final BiConsumer<Task, Boolean> onToggle;
    private final Consumer<Task> onEdit;

    public KanbanTaskCell(BiConsumer<Task, Boolean> onToggle, Consumer<Task> onEdit) {
        this.onToggle = onToggle;
        this.onEdit = onEdit;

        titleLabel.getStyleClass().add("task-title");
        titleLabel.setWrapText(true);
        titleLabel.setMinWidth(0);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        editButton.getStyleClass().add("icon-button");
        priorityBadge.getStyleClass().add("priority-badge");
        recurrenceBadge.getStyleClass().add("recurrence-badge");
        subtaskBadge.getStyleClass().add("subtask-count-badge");

        HBox topRow = new HBox(8, doneCheckBox, titleLabel, editButton);
        topRow.setAlignment(Pos.CENTER_LEFT);

        FlowPane badgeRow = new FlowPane(6, 4, priorityBadge, recurrenceBadge, subtaskBadge);
        badgeRow.setAlignment(Pos.CENTER_RIGHT);

        root = new VBox(8, topRow, badgeRow);
        root.getStyleClass().add("task-row");
        root.setPadding(new Insets(10, 12, 10, 12));
        root.setMaxWidth(Double.MAX_VALUE);
        setMaxWidth(Double.MAX_VALUE);

        detailTooltip.setShowDelay(Duration.millis(400));
        detailTooltip.setShowDuration(Duration.seconds(30));
        detailTooltip.getStyleClass().add("task-preview-tooltip");
        setTooltip(detailTooltip);

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
            setTooltip(null);
            return;
        }
        setTooltip(detailTooltip);
        detailTooltip.setGraphic(TaskPreview.build(task));

        doneCheckBox.setSelected(task.isCompleted());
        titleLabel.setText(task.getTitle());
        titleLabel.getStyleClass().removeAll("task-title-completed");
        if (task.isCompleted()) {
            titleLabel.getStyleClass().add("task-title-completed");
        }

        priorityBadge.setText(task.getPriority().toString());
        priorityBadge.getStyleClass().removeIf(c -> c.startsWith("priority-") && !c.equals("priority-badge"));
        priorityBadge.getStyleClass().add("priority-" + task.getPriority().name().toLowerCase(Locale.ROOT));

        boolean recurring = task.getRecurrence() != Recurrence.AUCUNE;
        recurrenceBadge.setText("🔁 " + task.getRecurrence());
        recurrenceBadge.setVisible(recurring);
        recurrenceBadge.setManaged(recurring);

        boolean hasSubtasks = !task.getSubtasks().isEmpty();
        if (hasSubtasks) {
            long done = task.getSubtasks().stream().filter(SubTask::isCompleted).count();
            subtaskBadge.setText(done + "/" + task.getSubtasks().size());
        }
        subtaskBadge.setVisible(hasSubtasks);
        subtaskBadge.setManaged(hasSubtasks);

        setGraphic(root);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }
}
