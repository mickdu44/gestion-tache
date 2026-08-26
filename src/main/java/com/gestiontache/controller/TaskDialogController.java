package com.gestiontache.controller;

import com.gestiontache.model.Priority;
import com.gestiontache.model.Recurrence;
import com.gestiontache.model.SubTask;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.IndexRange;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private VBox subtasksBox;

    @FXML
    private TextField newSubtaskField;

    @FXML
    private VBox attachmentsBox;

    private final List<SubTask> subtasks = new ArrayList<>();
    private final List<String> attachments = new ArrayList<>();

    @FXML
    private void initialize() {
        priorityComboBox.getItems().setAll(Priority.values());
        recurrenceComboBox.getItems().setAll(Recurrence.values());

        Tooltip markdownHint = new Tooltip(
                "Mise en forme prise en charge : **gras**, *italique*, \"- \" pour une liste, [texte](url) pour un lien.");
        markdownHint.setShowDelay(javafx.util.Duration.millis(200));
        descriptionArea.setTooltip(markdownHint);
        descriptionArea.setPromptText("Astuce : **gras**, *italique*, \"- \" pour une liste, [texte](url) pour un lien.");
    }

    public void fill(String title, String description, Priority priority, Recurrence recurrence, LocalDate date,
                      List<SubTask> existingSubtasks, List<String> existingAttachments) {
        titleField.setText(title == null ? "" : title);
        descriptionArea.setText(description == null ? "" : description);
        priorityComboBox.setValue(priority == null ? Priority.MOYENNE : priority);
        recurrenceComboBox.setValue(recurrence == null ? Recurrence.AUCUNE : recurrence);
        datePicker.setValue(date);

        subtasks.clear();
        if (existingSubtasks != null) {
            for (SubTask subTask : existingSubtasks) {
                SubTask copy = new SubTask();
                copy.setId(subTask.getId());
                copy.setTitle(subTask.getTitle());
                copy.setCompleted(subTask.isCompleted());
                subtasks.add(copy);
            }
        }
        renderSubtasks();

        attachments.clear();
        if (existingAttachments != null) {
            attachments.addAll(existingAttachments);
        }
        renderAttachments();
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

    public List<SubTask> getSubtasks() {
        return subtasks;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    @FXML
    private void onFormatBold() {
        wrapSelection("**", "**");
    }

    @FXML
    private void onFormatItalic() {
        wrapSelection("*", "*");
    }

    @FXML
    private void onFormatBullet() {
        insertBulletPrefix();
    }

    @FXML
    private void onFormatLink() {
        insertLink();
    }

    private void wrapSelection(String prefix, String suffix) {
        IndexRange selection = descriptionArea.getSelection();
        if (selection.getLength() > 0) {
            String selected = descriptionArea.getSelectedText();
            descriptionArea.replaceSelection(prefix + selected + suffix);
        } else {
            int caret = descriptionArea.getCaretPosition();
            descriptionArea.insertText(caret, prefix + suffix);
            descriptionArea.positionCaret(caret + prefix.length());
        }
        descriptionArea.requestFocus();
    }

    private void insertBulletPrefix() {
        String text = descriptionArea.getText();
        IndexRange selection = descriptionArea.getSelection();
        int lineStart = text.lastIndexOf('\n', Math.max(0, selection.getStart() - 1)) + 1;

        if (selection.getLength() > 0 && text.substring(selection.getStart(), selection.getEnd()).contains("\n")) {
            String block = text.substring(lineStart, selection.getEnd());
            String[] lines = block.split("\n", -1);
            StringBuilder rebuilt = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                rebuilt.append("- ").append(lines[i]);
                if (i < lines.length - 1) {
                    rebuilt.append("\n");
                }
            }
            descriptionArea.selectRange(lineStart, selection.getEnd());
            descriptionArea.replaceSelection(rebuilt.toString());
        } else {
            descriptionArea.insertText(lineStart, "- ");
        }
        descriptionArea.requestFocus();
    }

    private void insertLink() {
        String selected = descriptionArea.getSelectedText();
        TextInputDialog urlDialog = new TextInputDialog("https://");
        urlDialog.setTitle("Inserer un lien");
        urlDialog.setHeaderText(null);
        urlDialog.setContentText("URL du lien :");
        Optional<String> result = urlDialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            return;
        }
        String url = result.get().trim();
        String label = (selected == null || selected.isBlank()) ? "lien" : selected;
        String markdown = "[" + label + "](" + url + ")";
        if (descriptionArea.getSelection().getLength() > 0) {
            descriptionArea.replaceSelection(markdown);
        } else {
            int caret = descriptionArea.getCaretPosition();
            descriptionArea.insertText(caret, markdown);
        }
        descriptionArea.requestFocus();
    }

    @FXML
    private void onAddSubtask() {
        String title = newSubtaskField.getText().trim();
        if (title.isEmpty()) {
            return;
        }
        subtasks.add(new SubTask(title));
        newSubtaskField.clear();
        renderSubtasks();
    }

    private void renderSubtasks() {
        List<Node> rows = new ArrayList<>();
        for (SubTask subtask : subtasks) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(subtask.isCompleted());
            checkBox.setOnAction(e -> subtask.setCompleted(checkBox.isSelected()));

            Label label = new Label(subtask.getTitle());
            label.getStyleClass().add("subtask-edit-label");
            HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);

            Button remove = new Button("✕");
            remove.getStyleClass().add("icon-button");
            remove.setOnAction(e -> {
                subtasks.remove(subtask);
                renderSubtasks();
            });

            HBox row = new HBox(8, checkBox, label, remove);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("subtask-edit-row");
            rows.add(row);
        }
        subtasksBox.getChildren().setAll(rows);
    }

    @FXML
    private void onAddAttachment() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une piece jointe");
        File file = chooser.showOpenDialog(descriptionArea.getScene().getWindow());
        if (file != null) {
            attachments.add(file.getAbsolutePath());
            renderAttachments();
        }
    }

    private void renderAttachments() {
        List<Node> rows = new ArrayList<>();
        for (String path : attachments) {
            Label label = new Label(new File(path).getName());
            label.getStyleClass().add("attachment-chip-label");
            HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);

            Button remove = new Button("✕");
            remove.getStyleClass().add("icon-button");
            remove.setOnAction(e -> {
                attachments.remove(path);
                renderAttachments();
            });

            HBox row = new HBox(8, label, remove);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("attachment-edit-row");
            rows.add(row);
        }
        attachmentsBox.getChildren().setAll(rows);
    }
}
