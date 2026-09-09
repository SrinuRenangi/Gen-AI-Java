package com.genai.langchain4j.tools;

import java.util.Map;

/**
 * Model request to execute a registered tool with specified arguments.
 * Matches dev.langchain4j.agent.tool.ToolExecutionRequest.
 */
public record ToolExecutionRequest(
    String id,
    String name,
    Map<String, Object> arguments
) {}
