package com.genai.springai.tools;

import java.util.Map;

/**
 * Metadata definition of an executable tool passed to an LLM.
 * Mirroring the OpenAI / Spring AI function calling specification.
 */
public record ToolDefinition(
    String name,
    String description,
    Map<String, ParameterSpec> parameters
) {
    public record ParameterSpec(
        String type,
        String description,
        boolean required
    ) {}
}
