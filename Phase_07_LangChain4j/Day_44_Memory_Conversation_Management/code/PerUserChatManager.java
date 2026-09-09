package com.genai.langchain4j.memory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Enterprise multi-user chat manager routing distinct user IDs to isolated ChatMemory instances.
 */
public class PerUserChatManager {

    private final ChatMemoryProvider provider;
    private final Map<Object, ChatMemory> sessionCache = new ConcurrentHashMap<>();

    public PerUserChatManager(ChatMemoryProvider provider) {
        this.provider = provider;
    }

    public ChatMemory getMemoryForUser(Object userId) {
        return sessionCache.computeIfAbsent(userId, provider::get);
    }

    public String chat(Object userId, String userMessage) {
        ChatMemory memory = getMemoryForUser(userId);
        memory.add(ChatMessage.user(userMessage));

        // Generate response incorporating conversational history
        StringBuilder context = new StringBuilder();
        for (ChatMessage msg : memory.messages()) {
            context.append(msg.role()).append(": ").append(msg.text()).append(" | ");
        }

        String aiResponse = "Acknowledged: '" + userMessage + "'. Total history length for " + userId + ": " + memory.messages().size() + " messages.";
        memory.add(ChatMessage.ai(aiResponse));

        return aiResponse;
    }
}
