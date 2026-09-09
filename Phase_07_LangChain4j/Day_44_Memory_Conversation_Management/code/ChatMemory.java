package com.genai.langchain4j.memory;

import java.util.List;

/**
 * Standard contract for managing conversational history.
 * Matches dev.langchain4j.memory.ChatMemory.
 */
public interface ChatMemory {
    Object id();
    void add(ChatMessage message);
    List<ChatMessage> messages();
    void clear();
}
