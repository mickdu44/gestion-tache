package com.gestiontache.controller;

import com.gestiontache.util.DescriptionFormatter;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.Optional;

/**
 * A small rich-text editor for task descriptions, in two modes: a rendered
 * Markdown "consultation" view (shown by default, using
 * {@link DescriptionFormatter}) that switches to a raw "modification" view
 * (a formatting toolbar above a {@link TextArea}, holding the unrendered
 * markdown source) when clicked, and back to the rendered view once the
 * text area loses focus. Shared by the task creation dialog and the inline
 * detail-panel editor so the formatting logic only lives in one place.
 */
public class DescriptionEditorControl extends VBox {

    private static final String EMPTY_PLACEHOLDER = "(Aucune description)";

    private final TextArea descriptionArea = new TextArea();
    private final TextFlow previewFlow = new TextFlow();
    private final ScrollPane previewScroll = new ScrollPane(previewFlow);
    private final VBox editBox;

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
        descriptionArea.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                // Defer the check: a mouse click on a toolbar button also
                // blurs descriptionArea for an instant (the button grabs
                // focus first), before the button's action runs and calls
                // descriptionArea.requestFocus() again. Only leave edit mode
                // once focus has actually settled outside editBox.
                Platform.runLater(() -> {
                    if (!isWithinEditBox(currentFocusOwner())) {
                        showPreview();
                    }
                });
            }
        });

        VBox.setVgrow(descriptionArea, Priority.ALWAYS);
        editBox = new VBox(4, toolbar, descriptionArea);
        VBox.setVgrow(editBox, Priority.ALWAYS);

        previewFlow.getStyleClass().add("description-preview");
        previewScroll.setFitToWidth(true);
        previewScroll.getStyleClass().add("description-preview-scroll");
        previewScroll.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Hyperlink)) {
                enterEditMode();
            }
        });
        VBox.setVgrow(previewScroll, Priority.ALWAYS);

        setSpacing(4);
        showPreview();
    }

    public TextArea getDescriptionArea() {
        return descriptionArea;
    }

    public String getText() {
        return descriptionArea.getText().trim();
    }

    public void setText(String text) {
        descriptionArea.setText(text == null ? "" : text);
        showPreview();
    }

    /** Switches to the raw, editable Markdown source view and focuses it. */
    private void enterEditMode() {
        getChildren().setAll(editBox);
        descriptionArea.requestFocus();
        descriptionArea.positionCaret(descriptionArea.getText().length());
    }

    private Node currentFocusOwner() {
        Scene scene = descriptionArea.getScene();
        return scene != null ? scene.getFocusOwner() : null;
    }

    /** True if {@code node} is the toolbar/text area box itself or one of its descendants. */
    private boolean isWithinEditBox(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current == editBox) {
                return true;
            }
        }
        return false;
    }

    /** Switches to the rendered Markdown "consultation" view. */
    private void showPreview() {
        String text = descriptionArea.getText();
        if (text == null || text.isBlank()) {
            Text placeholder = new Text(EMPTY_PLACEHOLDER);
            placeholder.getStyleClass().add("detail-description-empty");
            previewFlow.getChildren().setAll(placeholder);
        } else {
            previewFlow.getChildren().setAll(DescriptionFormatter.toNodes(text));
        }
        getChildren().setAll(previewScroll);
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
        urlDialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/gestiontache/style.css").toExternalForm());
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
