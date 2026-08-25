package com.gestiontache.controller;

import javafx.fxml.FXML;
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
    private DatePicker datePicker;

    public void fill(String title, String description, LocalDate date) {
        titleField.setText(title == null ? "" : title);
        descriptionArea.setText(description == null ? "" : description);
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

    public LocalDate getDate() {
        return datePicker.getValue();
    }
}
