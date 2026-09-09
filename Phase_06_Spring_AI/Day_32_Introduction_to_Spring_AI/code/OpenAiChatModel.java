package com.genai.springai.core;

import java.util.List;

/**
 * Simulates Spring AI's OpenAiChatModel for cloud commercial inference (GPT-4o, o1, etc.).
 */
public class OpenAiChatModel implements ChatModel {

    private final String apiKey;
    private final String modelName;

    public OpenAiChatModel(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    public OpenAiChatModel() {
        this("sk-simulated-key-2026", "gpt-4o");
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

        long promptTokens = (systemInstruction.length() + userQuery.length()) / 4 + 15;
        String responseContent = "[OpenAI - " + modelName + "] Advanced reasoning output for: '" + userQuery + "'"
                + (systemInstruction.isEmpty() ? "" : " guided by system rule: [" + systemInstruction + "]");
        long completionTokens = responseContent.length() / 4 + 10;

        Message.AssistantMessage assistantMsg = new Message.AssistantMessage(responseContent);
        ChatResponse.Generation generation = new ChatResponse.Generation(assistantMsg, "STOP");
        ChatResponse.UsageMetadata usage = new ChatResponse.UsageMetadata(
                promptTokens, completionTokens, promptTokens + completionTokens
        );

        return new ChatResponse(List.of(generation), usage);
    }

    @Override
    public String getProviderName() {
        return "OpenAI (Cloud - " + modelName + ")";
    }
}
