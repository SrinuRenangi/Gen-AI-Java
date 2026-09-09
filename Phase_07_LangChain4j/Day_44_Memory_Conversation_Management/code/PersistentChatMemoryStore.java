package com.genai.langchain4j.memory;

import java.util.*;

/**
 * Thread-safe persistent in-memory store simulating external database backing.
 */
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final Map<Object, List<ChatMessage>> storage = new HashMap<>();

    @Override
    public synchronized List<ChatMessage> getMessages(Object memoryId) {
        return new ArrayList<>(storage.getOrDefault(memoryId, List.of()));
    }

    @Override
    public synchronized void updateMessages(Object memoryId, List<ChatMessage> messages) {
        storage.put(memoryId, new ArrayList<>(messages));
    }

    @Override
    public synchronized void deleteMessages(Object memoryId) {
        storage.remove(memoryId);
    }

    public synchronized int activeSessionCount() {
        return storage.size();
    }
}
