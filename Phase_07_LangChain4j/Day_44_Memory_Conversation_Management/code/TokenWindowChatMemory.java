package com.genai.langchain4j.memory;

import java.util.*;

/**
 * Chat memory retaining messages within a strict token budget.
 * Automatically evicts oldest conversational turns when total tokens exceed budget,
 * while preserving the foundational SystemMessage.
 */
public class TokenWindowChatMemory implements ChatMemory {

    private final Object id;
    private final int maxTokens;
    private final ChatMemoryStore store;

    public TokenWindowChatMemory(Object id, int maxTokens, ChatMemoryStore store) {
        this.id = id;
        this.maxTokens = maxTokens;
        this.store = store;
    }

    public static TokenWindowChatMemory withMaxTokens(int maxTokens) {
        return new TokenWindowChatMemory("default", maxTokens, new PersistentChatMemoryStore());
    }

    @Override
    public Object id() { return id; }

    @Override
    public synchronized void add(ChatMessage message) {
        List<ChatMessage> list = store.getMessages(id);
        list.add(message);
        ensureTokenLimit(list);
        store.updateMessages(id, list);
    }

    @Override
    public synchronized List<ChatMessage> messages() {
        return Collections.unmodifiableList(store.getMessages(id));
    }

    @Override
    public synchronized void clear() {
        store.deleteMessages(id);
    }

    public synchronized int totalTokens() {
        return store.getMessages(id).stream().mapToInt(ChatMessage::estimatedTokens).sum();
    }

    private void ensureTokenLimit(List<ChatMessage> list) {
        int currentTokens = list.stream().mapToInt(ChatMessage::estimatedTokens).sum();
        if (currentTokens <= maxTokens) return;

        boolean hasSystem = (!list.isEmpty() && list.get(0).role() == ChatMessage.Role.SYSTEM);

        while (currentTokens > maxTokens && list.size() > 1) {
            int removeIdx = (hasSystem && list.size() > 1) ? 1 : 0;
            ChatMessage removed = list.remove(removeIdx);
            currentTokens -= removed.estimatedTokens();
        }
    }
}
