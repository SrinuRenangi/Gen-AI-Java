package com.genai.enterprise.multiagent;

/**
 * Roles and specialized system prompts in a multi-agent hierarchy.
 */
public enum AgentRole {
    SUPERVISOR("Supervisor", "Deconstructs enterprise requirements into atomic subtasks and delegates to specialists."),
    RESEARCHER("Architect_Researcher", "Researches optimal algorithmic patterns, data structures, and trade-offs."),
    CODER("Software_Engineer", "Produces clean, idiomatic Java 21 production source code conforming to specifications."),
    SECURITY_AUDITOR("Security_Officer", "Performs vulnerability audits, concurrency review, and threat modeling.");

    private final String title;
    private final String description;

    AgentRole(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
