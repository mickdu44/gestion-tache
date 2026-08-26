package com.gestiontache.util;

import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a small Markdown-like subset supported in task descriptions:
 * {@code **gras**}, {@code *italique*}, {@code [texte](url)} links and lines
 * starting with {@code "- "} for a bullet list. Parsing logic is kept free
 * of JavaFX scene classes so it can be unit tested without the JavaFX
 * toolkit; only {@link #toNodes} builds actual scene nodes for display.
 */
public final class DescriptionFormatter {

    private static final Pattern INLINE_PATTERN =
            Pattern.compile("\\*\\*([^*]+)\\*\\*|\\*([^*]+)\\*|\\[([^\\]]+)\\]\\(([^)]+)\\)");

    private DescriptionFormatter() {
    }

    /**
     * One run of text with its bold/italic styling, in reading order.
     * {@code url} is non-null only for a link segment (its text is the link
     * label, never bold or italic at the same time).
     */
    public record Segment(String text, boolean bold, boolean italic, String url) {
        public Segment(String text, boolean bold, boolean italic) {
            this(text, bold, italic, null);
        }
    }

    public static List<Segment> parse(String description) {
        List<Segment> segments = new ArrayList<>();
        if (description == null || description.isEmpty()) {
            return segments;
        }
        String[] lines = description.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("- ")) {
                segments.add(new Segment("• ", false, false));
                line = line.substring(2);
            }
            segments.addAll(parseInline(line));
            if (i < lines.length - 1) {
                segments.add(new Segment("\n", false, false));
            }
        }
        return segments;
    }

    private static List<Segment> parseInline(String line) {
        List<Segment> result = new ArrayList<>();
        Matcher matcher = INLINE_PATTERN.matcher(line);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                result.add(new Segment(line.substring(lastEnd, matcher.start()), false, false));
            }
            if (matcher.group(1) != null) {
                result.add(new Segment(matcher.group(1), true, false));
            } else if (matcher.group(2) != null) {
                result.add(new Segment(matcher.group(2), false, true));
            } else {
                result.add(new Segment(matcher.group(3), false, false, matcher.group(4)));
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < line.length()) {
            result.add(new Segment(line.substring(lastEnd), false, false));
        }
        return result;
    }

    /** Strips the markdown markers, keeping bullet prefixes and line breaks, for compact previews. */
    public static String toPlainText(String description) {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : parse(description)) {
            sb.append(segment.text());
        }
        return sb.toString();
    }

    /** Builds styled nodes (text runs and links) ready to be added to a {@code TextFlow}. */
    public static List<Node> toNodes(String description) {
        List<Node> nodes = new ArrayList<>();
        for (Segment segment : parse(description)) {
            if (segment.url() != null) {
                Hyperlink link = new Hyperlink(segment.text());
                link.getStyleClass().add("md-link");
                link.setOnAction(e -> openLink(segment.url()));
                nodes.add(link);
                continue;
            }
            Text text = new Text(segment.text());
            text.getStyleClass().add("md-text");
            if (segment.bold()) {
                text.getStyleClass().add("md-bold");
            }
            if (segment.italic()) {
                text.getStyleClass().add("md-italic");
            }
            nodes.add(text);
        }
        return nodes;
    }

    private static void openLink(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) {
            // No default browser available in this environment; nothing sensible to fall back to.
        }
    }
}
