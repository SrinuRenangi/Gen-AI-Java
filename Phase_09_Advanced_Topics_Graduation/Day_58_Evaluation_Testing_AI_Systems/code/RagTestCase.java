package com.genai.enterprise.evaluation;

/**
 * Benchmark test case representing a RAG transaction to be evaluated.
 */
public record RagTestCase(
        String testId,
        String userQuery,
        String retrievedContext,
        String generatedAnswer,
        String groundTruthReference
) {}
