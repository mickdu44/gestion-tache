package com.gestiontache.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescriptionFormatterTest {

    @Test
    void parsesBoldAndItalicSegments() {
        List<DescriptionFormatter.Segment> segments = DescriptionFormatter.parse("Un **mot en gras** et *en italique*.");

        assertEquals("Un ", segments.get(0).text());
        assertTrue(segments.stream().anyMatch(s -> s.bold() && s.text().equals("mot en gras")));
        assertTrue(segments.stream().anyMatch(s -> s.italic() && s.text().equals("en italique")));
    }

    @Test
    void convertsBulletLinesToPlainTextWithBulletMarker() {
        String description = "- premiere ligne\n- deuxieme ligne";

        String plain = DescriptionFormatter.toPlainText(description);

        assertEquals("• premiere ligne\n• deuxieme ligne", plain);
    }

    @Test
    void toPlainTextStripsMarkdownMarkers() {
        String plain = DescriptionFormatter.toPlainText("**Important** : *a faire* rapidement.");

        assertEquals("Important : a faire rapidement.", plain);
    }

    @Test
    void handlesNullAndEmptyDescription() {
        assertEquals("", DescriptionFormatter.toPlainText(null));
        assertEquals("", DescriptionFormatter.toPlainText(""));
        assertTrue(DescriptionFormatter.parse(null).isEmpty());
    }
}
