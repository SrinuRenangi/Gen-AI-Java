package com.genai.foundations.day05;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Day 05: Modern Java: Records, Optional, and Sealed Types in Memory.
 * Demonstrates records with compact constructors, null safety via Optional, sealed hierarchies, and pattern matching switch.
 */

// 1. Sealed Interface modeling domain events
sealed interface AiEvent permits PromptEvent, ResponseEvent, FailureEvent {}

// 2. Immutable Records with compact constructors
record PromptEvent(String query, int tokenBudget) implements AiEvent {
    public PromptEvent {
        Objects.requireNonNull(query, "query cannot be null");
        if (tokenBudget <= 0) throw new IllegalArgumentException("Tokens must be positive");
    }
}

record ResponseEvent(String answer, long durationMs) implements AiEvent {
    public ResponseEvent {
        Objects.requireNonNull(answer, "answer cannot be null");
    }
}

record FailureEvent(String reason, int statusCode) implements AiEvent {}

public class ModernJavaDemo {

    // Proper Optional usage: return type only
    public static Optional<AiEvent> processInput(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return Optional.empty(); // Clean absence signaling without null
        }
        return Optional.of(new PromptEvent(rawInput.trim(), 2048));
    }

    // Pattern matching in switch expression: Exhaustive without default branch
    public static String evaluateEvent(AiEvent event) {
        return switch (event) {
            case PromptEvent p   -> "Processing prompt (" + p.tokenBudget() + " max tokens): " + p.query();
            case ResponseEvent r -> "Generated in " + r.durationMs() + "ms: " + r.answer();
            case FailureEvent f  -> "Error (" + f.statusCode() + "): " + f.reason();
        };
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 05: MODERN JAVA MEMORY & SYNTAX DEMO       ");
        System.out.println("==================================================");

        // 1. Optional unwrap with lazy evaluation
        AiEvent event = processInput("Explain quantum entanglement")
            .orElseGet(() -> new FailureEvent("Default fallback event", 400));

        // 2. Exhaustive pattern matching evaluation
        String summary = evaluateEvent(event);
        System.out.println("1. Event Summary via Pattern Matching:");
        System.out.println("   " + summary);
        System.out.println();

        // 3. Text Block demonstration
        String configJson = """
            {
              "model": "claude-3-5-sonnet",
              "temperature": 0.2
            }
            """;
        System.out.println("2. Raw Text Block Config:");
        System.out.println(configJson.trim());
        System.out.println();

        // 4. Record shallow immutability & defensive copying demonstration
        record ChatContext(String systemPrompt, List<String> history) {
            public ChatContext {
                history = List.copyOf(history); // Deep defensive copy!
            }
        }

        ChatContext ctx = new ChatContext("Be helpful", List.of("Hi", "Hello"));
        System.out.println("3. Record State: " + ctx);
        System.out.println("==================================================");
    }
}
