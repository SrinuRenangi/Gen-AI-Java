package com.javagenai.day04;

import java.time.Instant;

public class AIResponse<T> {
    private final T payload;
    private final int promptTokens;
    private final int completionTokens;
    private final Instant createdAt;

    public AIResponse(T payload, int promptTokens, int completionTokens) {
        this.payload = payload;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.createdAt = Instant.now();
    }

    public T getPayload() {
        return payload;
    }

    public int getTotalTokens() {
        return promptTokens + completionTokens;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("AIResponse[tokens=%d, payload=%s]", getTotalTokens(), payload);
    }
}
