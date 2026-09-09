package com.javagenai.day03;

public class OpenAiChatModel implements ChatModel {
    private final String apiKey;

    public OpenAiChatModel(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String call(String prompt) {
        return "[OpenAI GPT-4o Response to: '" + prompt + "']";
    }
}
