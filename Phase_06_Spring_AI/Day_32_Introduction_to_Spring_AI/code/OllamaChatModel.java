package com.genai.springai.core;

import java.util.List;

/**
 * Simulates Spring AI's OllamaChatModel for local, private open-weight inference (Llama 3.2, Mistral).
 */
public class OllamaChatModel implements ChatModel {

    private final String baseUrl;
    private final String defaultModel;

    public OllamaChatModel(String baseUrl, String defaultModel) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
    }

    public OllamaChatModel() {
        this("http://localhost:11434", "llama3.2");
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        String systemInstruction = "";
        String userQuery = "";

        for (Message msg : prompt.messages()) {
            if (msg.getMessageType() == Message.MessageType.SYSTEM) {
                systemInstruction = msg.getContent();
            } else if (msg.getMessageType() == Message.MessageType.USER) {
                userQuery = msg.getContent();
            }
        }

        // Realistic token estimation
        long promptTokens = (systemInstruction.length() + userQuery.length()) / 4 + 10;
        
        String simulatedCompletion = generateOllamaResponse(systemInstruction, userQuery);
        long completionTokens = simulatedCompletion.length() / 4 + 8;

        Message.AssistantMessage assistantMsg = new Message.AssistantMessage(simulatedCompletion);
        ChatResponse.Generation generation = new ChatResponse.Generation(assistantMsg, "STOP");
        ChatResponse.UsageMetadata usage = new ChatResponse.UsageMetadata(
                promptTokens, completionTokens, promptTokens + completionTokens
        );

        return new ChatResponse(List.of(generation), usage);
    }

    private String generateOllamaResponse(String system, String query) {
        if (query.toLowerCase().contains("spring ai")) {
            return "[Ollama - Llama 3.2] Spring AI is the official Spring ecosystem framework for building AI applications in Java. "
                    + "It brings portable abstractions for ChatModels, VectorStores, and Document Readers, eliminating vendor lock-in!";
        }
        if (query.toLowerCase().contains("architecture")) {
            return "[Ollama - Llama 3.2] Spring AI's architecture is built on three pillars: "
                    + "1. Portable Model Client Abstractions (ChatModel, EmbeddingModel) "
                    + "2. Structured Prompts & Converters "
                    + "3. Vector Stores & RAG Pipeline components.";
        }
        return "[Ollama - Llama 3.2 (Local Engine)] Processed query: '" + query + "' "
                + (system.isEmpty() ? "" : "(Directive: " + system + ")");
    }

    @Override
    public String getProviderName() {
        return "Ollama (Local - " + defaultModel + " at " + baseUrl + ")";
    }
}
