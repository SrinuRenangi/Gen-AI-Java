package com.genai.langchain4j.aiservices;

import java.util.List;

/**
 * Standard interface for interacting with a chat language model.
 * Matches dev.langchain4j.model.chat.ChatLanguageModel.
 */
public interface ChatLanguageModel {
    ModelResponse generate(List<ChatMessage> messages);
}
