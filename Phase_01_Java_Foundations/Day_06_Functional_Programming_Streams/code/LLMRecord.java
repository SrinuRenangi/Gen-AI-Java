package com.javagenai.day06;

public record LLMRecord(String model, int promptTokens, int completionTokens) {
    public int totalTokens() {
        return promptTokens + completionTokens;
    }
}
