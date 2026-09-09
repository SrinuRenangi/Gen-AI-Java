package com.genai.langchain4j.react;

import java.util.Map;

/**
 * Encapsulates an action selected by the agent during the reasoning loop.
 */
public record AgentAction(String toolName, Map<String, Object> arguments) {
    public static final AgentAction FINISH = new AgentAction("FINISH", Map.of());

    public boolean isFinish() {
        return "FINISH".equalsIgnoreCase(toolName);
    }
}
