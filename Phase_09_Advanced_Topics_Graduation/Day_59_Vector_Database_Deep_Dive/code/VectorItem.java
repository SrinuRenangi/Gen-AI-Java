package com.genai.enterprise.vectordb;

import java.util.Arrays;

/**
 * High-dimensional vector item containing identifier, dense floating-point vector, and metadata.
 */
public record VectorItem(
        String id,
        String textPayload,
        float[] embedding
) {
    @Override
    public String toString() {
        return "VectorItem[id=" + id + ", dims=" + embedding.length + ", text='" + 
                (textPayload.length() > 40 ? textPayload.substring(0, 40) + "..." : textPayload) + "']";
    }
}
