package com.genai.springai.vectorstore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simulates Spring AI's Document class.
 * Encapsulates text content, an ID, vector embedding, and key-value metadata.
 */
public record Document(
        String id,
        String content,
        Map<String, Object> metadata,
        float[] embedding
) {
    public Document(String id, String content, Map<String, Object> metadata) {
        this(id, content, Collections.unmodifiableMap(new HashMap<>(metadata)), null);
    }

    public Document(String content, Map<String, Object> metadata) {
        this(UUID.randomUUID().toString(), content, metadata);
    }

    public Document(String content) {
        this(UUID.randomUUID().toString(), content, Map.of());
    }

    public Document withEmbedding(float[] newEmbedding) {
        return new Document(id, content, metadata, newEmbedding);
    }
}
