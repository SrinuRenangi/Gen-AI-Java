package com.javagenai.day02;

public class ChatMessage {
    private String role;
    private String content;
    private int tokenCount;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.tokenCount = estimateTokens(content);
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.tokenCount = estimateTokens(content);
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public boolean isSystemMessage() {
        return "system".equalsIgnoreCase(this.role);
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }

    @Override
    public String toString() {
        return String.format("ChatMessage[role='%s', tokens=%d, content='%s']", 
                             role, tokenCount, content);
    }
}
