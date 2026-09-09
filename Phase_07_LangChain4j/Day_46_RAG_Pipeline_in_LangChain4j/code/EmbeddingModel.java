package com.genai.langchain4j.rag;

import java.util.List;

/**
 * Functional contract for generating dense embeddings.
 * Matches dev.langchain4j.model.embedding.EmbeddingModel.
 */
public interface EmbeddingModel {
    Embedding embed(String text);
    List<Embedding> embedAll(List<TextSegment> textSegments);
}
