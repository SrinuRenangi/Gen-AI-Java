package com.genai.foundations;

/**
 * Day 01: Java Ecosystem, JVM Architecture, and Memory Hierarchy
 * Demonstrates variable allocations across Metaspace, Heap, and Stack Frames.
 */
public class AiMemoryTracker {

    // 1. Static constant: Lives in Metaspace (shared process-wide)
    public static final String DEFAULT_PROVIDER = "Anthropic";

    public static void main(String[] args) {
        // 2. Local primitive: Raw binary value 1500 stored directly in main's Stack Frame
        int maxTokens = 1500;

        // 3. Local reference: 'prompt' holds memory pointer (e.g. 0x7F01) in main's Stack Frame;
        //    The PromptPayload instance data lives on the Heap at address 0x7F01
        PromptPayload prompt = new PromptPayload("Summarize research paper", 0.7);

        // 4. Method call: Pushes a new Stack Frame for executePrompt()
        int usedTokens = executePrompt(prompt, maxTokens);

        System.out.println("Execution finished using tokens: " + usedTokens);
        System.out.println("Default Provider from Metaspace: " + DEFAULT_PROVIDER);
    }

    public static int executePrompt(PromptPayload payload, int tokenLimit) {
        // A new Stack Frame is created here!
        // 'payload' holds a COPIED pointer pointing to the same Heap object
        // 'tokenLimit' holds a COPIED primitive value (1500)
        boolean isSafe = payload.temperature() <= 1.0;
        int finalAllocation = isSafe ? tokenLimit : 0;
        return finalAllocation;
        // When this method returns, this Stack Frame is instantly popped and destroyed!
    }
}

// Immutable record representing prompt configuration
record PromptPayload(String query, double temperature) {}
