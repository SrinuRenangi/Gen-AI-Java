package com.genai.langchain4j.rag;

import java.util.Map;

/**
 * Represents a discrete chunk of text extracted from an enterprise document,
 * accompanied by source metadata.
 * Matches dev.langchain4j.data.segment.TextSegment.
 */
public record TextSegment(String text, Map<String, Object> metadata) {
    public static TextSegment from(String text, Map<String, Object> metadata) {
        return new TextSegment(text, metadata);
    }

    public static TextSegment from(String text) {
        return new TextSegment(text, Map.of());
    }
}
