package com.genai.springai.tools;

import java.util.Map;

/**
 * Functional contract for any Java business service exposed to the LLM.
 */
public interface FunctionTool {
    ToolDefinition getDefinition();
    String execute(Map<String, Object> arguments) throws Exception;
}
