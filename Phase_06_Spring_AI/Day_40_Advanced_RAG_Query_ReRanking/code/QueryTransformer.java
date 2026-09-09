package com.genai.springai.advancedrag;

/**
 * Interface representing a query pre-processing transformation in advanced RAG.
 */
public interface QueryTransformer {
    String transform(String query);
}
