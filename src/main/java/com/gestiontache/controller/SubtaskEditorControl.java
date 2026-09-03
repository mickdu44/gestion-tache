package com.gestiontache.controller;

import com.gestiontache.model.SubTask;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Editable checklist of {@link SubTask}s: add a new one, tick/untick, or
 * remove one. Shared by the task creation dialog (which only reads the
 * final list back on OK) and the inline detail-panel editor (which sets
 * {@link #setOnChange} to persist immediately on every mutation). Checked
 * subtasks are shown struck through and sorted after the unchecked ones,
 * without changing the underlying list order (kept as insertion order, the
 * same way completed tasks sort last in the main list without an actual
 * reorder).
 */
public class SubtaskEditorControl extends VBox {

    private final VBox rowsBox = new VBox(4);
    private final TextField newSubtaskField = new TextField();
    private final List<SubTask> subtasks = new ArrayList<>();
    private Runnable onChange;

    public SubtaskEditorControl() {
        newSubtaskField.setPromptText("Nouvelle sous-tache...");
        HBox.setHgrow(newSubtaskField, Priority.ALWAYS);
        Button addButton = new Button("Ajouter");
        addButton.setOnAction(e -> addSubtask());
        newSubtaskField.setOnAction(e -> addSubtask());

        HBox addRow = new HBox(6, newSubtaskField, addButton);
        addRow.setAlignment(Pos.CENTER_LEFT);

        setSpacing(4);
        getChildren().setAll(rowsBox, addRow);
    }

    /** Notified after every add/remove/toggle. Not called while {@link #setSubtasks} is loading a task. */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    /** Loads a deep copy of {@code existing} so edits here never mutate the caller's list until read back. */
    public void setSubtasks(List<SubTask> existing) {
        subtasks.clear();
        if (existing != null) {
            for (SubTask subTask : existing) {
                SubTask copy = new SubTask();
                copy.setId(subTask.getId());
                copy.setTitle(subTask.getTitle());
                copy.setCompleted(subTask.isCompleted());
                subtasks.add(copy);
            }
        }
        render();
    }

    public List<SubTask> getSubtasks() {
        return subtasks;
    }

    private void addSubtask() {
        String title = newSubtaskField.getText().trim();
        if (title.isEmpty()) {
            return;
        }
        subtasks.add(new SubTask(title));
        newSubtaskField.clear();
        render();
        fireChange();
    }

    private void render() {
        List<SubTask> ordered = new ArrayList<>(subtasks);
        ordered.sort(Comparator.comparing(SubTask::isCompleted));

        List<Node> rows = new ArrayList<>();
        for (SubTask subtask : ordered) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(subtask.isCompleted());
            checkBox.setOnAction(e -> {
                subtask.setCompleted(checkBox.isSelected());
                render();
                fireChange();
            });

            Label label = new Label(subtask.getTitle());
            label.getStyleClass().add("subtask-edit-label");
            if (subtask.isCompleted()) {
                label.getStyleClass().add("subtask-edit-label-completed");
            }
            HBox.setHgrow(label, Priority.ALWAYS);

            Button remove = new Button("✕");
            remove.getStyleClass().add("icon-button");
            remove.setOnAction(e -> {
                subtasks.remove(subtask);
                render();
                fireChange();
            });

            HBox row = new HBox(8, checkBox, label, remove);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("subtask-edit-row");
            rows.add(row);
        }
        rowsBox.getChildren().setAll(rows);
    }

    private void fireChange() {
        if (onChange != null) {
            onChange.run();
        }
    }
}
