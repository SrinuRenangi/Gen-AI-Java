package com.javagenai.day10;

import java.util.ArrayList;
import java.util.List;

public class ChatConversationSession {
    private final List<String> messageHistory = new ArrayList<>();

    public void addMessage(String msg) {
        messageHistory.add(msg);
    }

    public List<String> getHistory() {
        return List.copyOf(messageHistory);
    }
}
