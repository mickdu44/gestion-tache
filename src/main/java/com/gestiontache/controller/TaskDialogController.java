package com.gestiontache.controller;

import com.gestiontache.model.Priority;
import com.gestiontache.model.Recurrence;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;

public class TaskDialogController {

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<Priority> priorityComboBox;

    @FXML
    private ComboBox<Recurrence> recurrenceComboBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private void initialize() {
        priorityComboBox.getItems().setAll(Priority.values());
        recurrenceComboBox.getItems().setAll(Recurrence.values());
    }

    public void fill(String title, String description, Priority priority, Recurrence recurrence, LocalDate date) {
        titleField.setText(title == null ? "" : title);
        descriptionArea.setText(description == null ? "" : description);
        priorityComboBox.setValue(priority == null ? Priority.MOYENNE : priority);
        recurrenceComboBox.setValue(recurrence == null ? Recurrence.AUCUNE : recurrence);
        datePicker.setValue(date);
    }

    public TextField getTitleField() {
        return titleField;
    }

    public String getTitle() {
        return titleField.getText().trim();
    }

    public String getDescription() {
        return descriptionArea.getText().trim();
    }

    public Priority getPriority() {
        return priorityComboBox.getValue();
    }

    public Recurrence getRecurrence() {
        return recurrenceComboBox.getValue();
    }

    public LocalDate getDate() {
        return datePicker.getValue();
    }
}
