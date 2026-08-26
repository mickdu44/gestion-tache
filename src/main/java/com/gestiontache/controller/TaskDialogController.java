package com.gestiontache.controller;

import com.gestiontache.model.Priority;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

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
        Tooltip markdownHint = new Tooltip(
                "Mise en forme prise en charge : **gras**, *italique*, lignes commencant par \"- \" pour une liste.");
        markdownHint.setShowDelay(javafx.util.Duration.millis(200));
        descriptionArea.setTooltip(markdownHint);
        descriptionArea.setPromptText("Astuce : **gras**, *italique*, \"- \" pour une liste a puces.");
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
