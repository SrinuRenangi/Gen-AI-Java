package com.genai.langchain4j.memory;

/**
 * Message record representing individual turns in a conversational context.
 */
public record ChatMessage(Role role, String text) {
    public enum Role { SYSTEM, USER, AI }

    public static ChatMessage system(String text) { return new ChatMessage(Role.SYSTEM, text); }
    public static ChatMessage user(String text) { return new ChatMessage(Role.USER, text); }
    public static ChatMessage ai(String text) { return new ChatMessage(Role.AI, text); }

    public int estimatedTokens() {
        return Math.max(1, text.length() / 4);
    }
}
