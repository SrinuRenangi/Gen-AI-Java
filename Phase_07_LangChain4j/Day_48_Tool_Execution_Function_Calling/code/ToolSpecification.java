package com.genai.langchain4j.tools;

import java.util.Map;

/**
 * Metadata definition of an executable tool passed into model system prompts.
 * Matches dev.langchain4j.agent.tool.ToolSpecification.
 */
public record ToolSpecification(
    String name,
    String description,
    Map<String, ParameterInfo> parameters
) {
    public record ParameterInfo(String type, String description) {}
}
