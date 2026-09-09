package com.genai.langchain4j.react;

import java.util.List;

/**
 * Encapsulates the overall outcome of an autonomous agent execution run.
 */
public record AgentExecutionResult(
    String goal,
    String finalAnswer,
    List<AgentStep> steps,
    int iterations,
    boolean completedSuccessfully,
    String stopReason
) {}
