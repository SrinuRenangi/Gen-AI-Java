package com.genai.springai.core;

/**
 * The fundamental Model client interface in Spring AI.
 * Implementations exist for Ollama, OpenAI, Anthropic, Bedrock, Vertex AI, Azure, etc.
 */
public interface ChatModel {

    ChatResponse call(Prompt prompt);

    default String call(String message) {
        Prompt prompt = new Prompt(message);
        ChatResponse response = call(prompt);
        return response.getResult().output().getContent();
    }

    String getProviderName();
}
