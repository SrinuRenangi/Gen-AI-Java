package com.genai.langchain4j.memory;

import java.util.List;

/**
 * Persistence SPI for saving and restoring conversational turns across restarts.
 * Implementations can map to Redis, PostgreSQL, DynamoDB, MongoDB, etc.
 * Matches dev.langchain4j.store.memory.chat.ChatMemoryStore.
 */
public interface ChatMemoryStore {
    List<ChatMessage> getMessages(Object memoryId);
    void updateMessages(Object memoryId, List<ChatMessage> messages);
    void deleteMessages(Object memoryId);
}
