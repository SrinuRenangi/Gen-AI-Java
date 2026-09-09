package com.javagenai.day03;

public class OllamaChatModel implements ChatModel {
    private final String localHostUrl;

    public OllamaChatModel(String localHostUrl) {
        this.localHostUrl = localHostUrl;
    }

    @Override
    public String call(String prompt) {
        return "[Ollama Llama-3.2 Local Response to: '" + prompt + "']";
    }
}
