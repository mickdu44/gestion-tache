package com.gestiontache.controller;

import com.gestiontache.model.Priority;
import com.gestiontache.model.Recurrence;
import com.gestiontache.model.SubTask;
import com.gestiontache.model.TaskStatus;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;

public class TaskDialogController {

    @FXML
    private TextField titleField;

    @FXML
    private DescriptionEditorControl descriptionEditor;

    @FXML
    private ComboBox<Priority> priorityComboBox;

    @FXML
    private ComboBox<Recurrence> recurrenceComboBox;

    @FXML
    private ComboBox<TaskStatus> statusComboBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private SubtaskEditorControl subtaskEditor;

    @FXML
    private AttachmentEditorControl attachmentEditor;

    @FXML
    private void initialize() {
        priorityComboBox.getItems().setAll(Priority.values());
        recurrenceComboBox.getItems().setAll(Recurrence.values());
        statusComboBox.getItems().setAll(TaskStatus.values());
    }

    public void fill(String title, String description, Priority priority, Recurrence recurrence, TaskStatus status,
                      LocalDate date, List<SubTask> existingSubtasks, List<String> existingAttachments) {
        titleField.setText(title == null ? "" : title);
        descriptionEditor.setText(description);
        priorityComboBox.setValue(priority == null ? Priority.MOYENNE : priority);
        recurrenceComboBox.setValue(recurrence == null ? Recurrence.AUCUNE : recurrence);
        statusComboBox.setValue(status == null ? TaskStatus.A_FAIRE : status);
        datePicker.setValue(date);
        subtaskEditor.setSubtasks(existingSubtasks);
        attachmentEditor.setAttachments(existingAttachments);
    }

    public TextField getTitleField() {
        return titleField;
    }

    public String getTitle() {
        return titleField.getText().trim();
    }

    public String getDescription() {
        return descriptionEditor.getText();
    }

    public Priority getPriority() {
        return priorityComboBox.getValue();
    }

    public Recurrence getRecurrence() {
        return recurrenceComboBox.getValue();
    }

    public TaskStatus getStatus() {
        return statusComboBox.getValue();
    }

    public LocalDate getDate() {
        return datePicker.getValue();
    }

    public List<SubTask> getSubtasks() {
        return subtaskEditor.getSubtasks();
    }

    public List<String> getAttachments() {
        return attachmentEditor.getAttachments();
    }
}
