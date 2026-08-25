package com.gestiontache.controller;

import com.gestiontache.model.Priority;
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
    private DatePicker datePicker;

    @FXML
    private void initialize() {
        priorityComboBox.getItems().setAll(Priority.values());
    }

    public void fill(String title, String description, Priority priority, LocalDate date) {
        titleField.setText(title == null ? "" : title);
        descriptionArea.setText(description == null ? "" : description);
        priorityComboBox.setValue(priority == null ? Priority.MOYENNE : priority);
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

    public LocalDate getDate() {
        return datePicker.getValue();
    }
}
