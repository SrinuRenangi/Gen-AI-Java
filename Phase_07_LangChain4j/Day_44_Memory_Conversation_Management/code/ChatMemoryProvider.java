package com.genai.langchain4j.memory;

/**
 * Functional factory for providing isolated ChatMemory instances per session or tenant.
 * Matches dev.langchain4j.memory.chat.ChatMemoryProvider.
 */
@FunctionalInterface
public interface ChatMemoryProvider {
    ChatMemory get(Object memoryId);
}
