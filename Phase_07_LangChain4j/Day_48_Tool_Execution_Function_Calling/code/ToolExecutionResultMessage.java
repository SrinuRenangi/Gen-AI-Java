package com.genai.langchain4j.tools;

/**
 * Encapsulates the execution result of a tool passed back to the model.
 * Matches dev.langchain4j.data.message.ToolExecutionResultMessage.
 */
public record ToolExecutionResultMessage(
    String id,
    String toolName,
    String resultText
) {}
