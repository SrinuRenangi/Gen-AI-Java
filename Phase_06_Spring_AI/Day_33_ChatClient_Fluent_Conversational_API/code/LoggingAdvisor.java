package com.genai.springai.chatclient;

import com.genai.springai.core.ChatResponse;
import com.genai.springai.core.Prompt;

/**
 * Simulates Spring AI's SimpleLoggerAdvisor.
 * Logs input prompts, response tokens, and elapsed latency.
 */
public class LoggingAdvisor implements Advisor {

    private long startTimeNanos;

    @Override
    public String getName() {
        return "SimpleLoggerAdvisor";
    }

    @Override
    public Prompt before(Prompt prompt) {
        this.startTimeNanos = System.nanoTime();
        System.out.println("  [ADVISOR: LOGGER] >>> Sending Prompt with " + prompt.messages().size() + " messages to Model.");
        return prompt;
    }

    @Override
    public ChatResponse after(ChatResponse response) {
        long elapsedMs = (System.nanoTime() - startTimeNanos) / 1_000_000;
        ChatResponse.UsageMetadata usage = response.usage();
        System.out.println("  [ADVISOR: LOGGER] <<< Received Response in " + elapsedMs + "ms. Tokens used: " 
                + (usage != null ? usage.totalTokens() : "N/A"));
        return response;
    }
}
