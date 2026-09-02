package com.gestiontache.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Editable list of local file attachments: add one via a native file
 * chooser, click one to open it with the OS's default application, or
 * remove one. Shared by the task creation dialog (which only reads the
 * final list back on OK) and the inline detail-panel editor (which sets
 * {@link #setOnChange} to persist immediately on every mutation).
 */
public class AttachmentEditorControl extends VBox {

    private final VBox rowsBox = new VBox(4);
    private final List<String> attachments = new ArrayList<>();
    private Runnable onChange;

    public AttachmentEditorControl() {
        Button addButton = new Button("Ajouter une piece jointe");
        addButton.setOnAction(e -> onAddAttachment());

        setSpacing(4);
        getChildren().setAll(rowsBox, addButton);
    }

    /** Notified after every add/remove. Not called while {@link #setAttachments} is loading a task. */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void setAttachments(List<String> existing) {
        attachments.clear();
        if (existing != null) {
            attachments.addAll(existing);
        }
        render();
    }

    public List<String> getAttachments() {
        return attachments;
    }

    private void onAddAttachment() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une piece jointe");
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            attachments.add(file.getAbsolutePath());
            render();
            fireChange();
        }
    }

    private void render() {
        List<Node> rows = new ArrayList<>();
        for (String path : attachments) {
            Hyperlink link = new Hyperlink(new File(path).getName());
            link.getStyleClass().add("attachment-link");
            link.setOnAction(e -> openAttachment(path));
            HBox.setHgrow(link, Priority.ALWAYS);

            Button remove = new Button("✕");
            remove.getStyleClass().add("icon-button");
            remove.setOnAction(e -> {
                attachments.remove(path);
                render();
                fireChange();
            });

            HBox row = new HBox(8, link, remove);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("attachment-edit-row");
            rows.add(row);
        }
        rowsBox.getChildren().setAll(rows);
    }

    private void openAttachment(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Gestion des taches");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir le fichier : " + path);
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/gestiontache/style.css").toExternalForm());
            alert.showAndWait();
        }
    }

    private void fireChange() {
        if (onChange != null) {
            onChange.run();
        }
    }
}
