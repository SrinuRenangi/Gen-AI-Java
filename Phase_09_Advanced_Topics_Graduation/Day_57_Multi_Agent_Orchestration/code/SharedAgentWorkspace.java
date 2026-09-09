package com.genai.enterprise.multiagent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe shared Blackboard workspace holding collaborative agent state and artifacts.
 */
public class SharedAgentWorkspace {

    private final String missionGoal;
    private final List<AgentMessage> messageLog = new CopyOnWriteArrayList<>();
    private final Map<String, String> artifacts = new ConcurrentHashMap<>();
    private volatile boolean approved = false;

    public SharedAgentWorkspace(String missionGoal) {
        this.missionGoal = missionGoal;
    }

    public void logMessage(AgentRole sender, AgentRole recipient, String content) {
        messageLog.add(AgentMessage.of(sender, recipient, content));
    }

    public void putArtifact(String key, String content) {
        artifacts.put(key, content);
    }

    public String getArtifact(String key) {
        return artifacts.get(key);
    }

    public String getMissionGoal() { return missionGoal; }
    public List<AgentMessage> getMessageLog() { return Collections.unmodifiableList(messageLog); }
    public Map<String, String> getArtifacts() { return Collections.unmodifiableMap(artifacts); }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
}
