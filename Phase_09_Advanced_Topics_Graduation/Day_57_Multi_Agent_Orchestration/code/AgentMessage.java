package com.genai.enterprise.multiagent;

/**
 * Message exchanged across the multi-agent orchestration blackboard.
 */
public record AgentMessage(
        AgentRole sender,
        AgentRole recipient,
        String content,
        long epochMs
) {
    public static AgentMessage of(AgentRole sender, AgentRole recipient, String content) {
        return new AgentMessage(sender, recipient, content, System.currentTimeMillis());
    }
}
