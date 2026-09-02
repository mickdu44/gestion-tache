package com.gestiontache.controller;

import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Optional;

/**
 * A small rich-text editor: a formatting toolbar (bold, italic, bullet
 * list, link) above a {@link TextArea}, inserting the markdown syntax that
 * {@link com.gestiontache.util.DescriptionFormatter} understands. Shared by
 * the task creation dialog and the inline detail-panel editor so the
 * formatting logic only lives in one place.
 */
public class DescriptionEditorControl extends VBox {

    private final TextArea descriptionArea = new TextArea();

    public DescriptionEditorControl() {
        Button boldButton = new Button("G");
        Button italicButton = new Button("I");
        Button bulletButton = new Button("Liste");
        Button linkButton = new Button("Lien");
        boldButton.getStyleClass().add("format-button");
        italicButton.getStyleClass().add("format-button");
        bulletButton.getStyleClass().add("format-button");
        linkButton.getStyleClass().add("format-button");
        boldButton.setOnAction(e -> wrapSelection("**", "**"));
        italicButton.setOnAction(e -> wrapSelection("*", "*"));
        bulletButton.setOnAction(e -> insertBulletPrefix());
        linkButton.setOnAction(e -> insertLink());

        HBox toolbar = new HBox(4, boldButton, italicButton, bulletButton, linkButton);
        toolbar.getStyleClass().add("format-toolbar");

        descriptionArea.setPrefRowCount(5);
        descriptionArea.setWrapText(true);
        Tooltip markdownHint = new Tooltip(
                "Mise en forme prise en charge : **gras**, *italique*, \"- \" pour une liste, [texte](url) pour un lien.");
        markdownHint.setShowDelay(Duration.millis(200));
        descriptionArea.setTooltip(markdownHint);
        descriptionArea.setPromptText("Astuce : **gras**, *italique*, \"- \" pour une liste, [texte](url) pour un lien.");

        VBox.setVgrow(descriptionArea, javafx.scene.layout.Priority.ALWAYS);
        setSpacing(4);
        getChildren().setAll(toolbar, descriptionArea);
    }

    public TextArea getDescriptionArea() {
        return descriptionArea;
    }

    public String getText() {
        return descriptionArea.getText().trim();
    }

    public void setText(String text) {
        descriptionArea.setText(text == null ? "" : text);
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
}
