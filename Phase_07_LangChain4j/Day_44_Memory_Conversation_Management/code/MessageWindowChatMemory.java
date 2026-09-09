package com.genai.langchain4j.memory;

import java.util.*;

/**
 * Chat memory retaining up to N total messages.
 * Critical invariant: The leading SystemMessage is NEVER evicted,
 * ensuring foundational agent constraints remain permanent.
 */
public class MessageWindowChatMemory implements ChatMemory {

    private final Object id;
    private final int maxMessages;
    private final ChatMemoryStore store;

    public MessageWindowChatMemory(Object id, int maxMessages, ChatMemoryStore store) {
        this.id = id;
        this.maxMessages = maxMessages;
        this.store = store;
    }

    public static MessageWindowChatMemory withMaxMessages(int maxMessages) {
        return new MessageWindowChatMemory("default", maxMessages, new PersistentChatMemoryStore());
    }

    @Override
    public Object id() { return id; }

    @Override
    public synchronized void add(ChatMessage message) {
        List<ChatMessage> list = store.getMessages(id);
        list.add(message);
        ensureCapacity(list);
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

    private void ensureCapacity(List<ChatMessage> list) {
        if (list.size() <= maxMessages) return;

        boolean hasSystem = (!list.isEmpty() && list.get(0).role() == ChatMessage.Role.SYSTEM);

        // Evict oldest non-system message
        while (list.size() > maxMessages) {
            int removeIdx = (hasSystem && list.size() > 1) ? 1 : 0;
            list.remove(removeIdx);
        }
    }
}
