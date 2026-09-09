package com.genai.langchain4j.react;

/**
 * Represents a single turn in the ReAct loop: Thought -> Action -> Observation.
 */
public record AgentStep(
    int stepNumber,
    String thought,
    AgentAction action,
    String observation
) {}
