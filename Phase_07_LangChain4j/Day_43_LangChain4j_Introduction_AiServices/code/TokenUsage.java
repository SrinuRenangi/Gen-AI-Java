package com.genai.langchain4j.aiservices;

/**
 * Tracks token consumption across model requests and completions.
 */
public record TokenUsage(int inputTokens, int outputTokens, int totalTokens) {}
