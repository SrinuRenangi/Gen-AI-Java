package com.genai.langchain4j.aiservices;

/**
 * Encapsulates the response payload from a ChatLanguageModel.
 */
public record ModelResponse(String content, TokenUsage tokenUsage) {}
