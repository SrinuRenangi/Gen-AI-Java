package com.javagenai.day03;

// Sealed interface hierarchy for AI stream events
sealed interface LLMEvent permits ChunkEvent, ErrorEvent, FinishedEvent {}

record ChunkEvent(String text) implements LLMEvent {}
record ErrorEvent(String errorMsg, int code) implements LLMEvent {}
record FinishedEvent(long durationMs) implements LLMEvent {}

public class PolymorphismDemo {

    public static void handleStreamEvent(LLMEvent event) {
        // Modern Java 21 exhaustive pattern matching switch
        switch (event) {
            case ChunkEvent chunk -> 
                System.out.print(chunk.text());
            case ErrorEvent err -> 
                System.err.printf("%n[ERROR %d]: %s%n", err.code(), err.errorMsg());
            case FinishedEvent done -> 
                System.out.printf("%n[STREAM FINISHED in %d ms]%n", done.durationMs());
        }
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 03: POLYMORPHISM & CONTRACT SYSTEM DEMO    ");
        System.out.println("==================================================");

        // 1. Swappable LLM Models via Polymorphism
        ChatModel openAi = new OpenAiChatModel("sk-test-key-12345");
        ChatModel ollama = new OllamaChatModel("http://localhost:11434");

        AIAssistantService bot = new AIAssistantService(openAi);
        System.out.println("1. Querying with OpenAI (Cloud):");
        System.out.println("   " + bot.answerUserQuery("What is RAG?"));
        System.out.println();

        System.out.println("2. Hot-swapping to Ollama (Local) with ZERO code change:");
        bot.setChatModel(ollama);
        System.out.println("   " + bot.answerUserQuery("What is RAG?"));
        System.out.println();

        // 2. Modern Java 21 Sealed Interface & Pattern Matching Demo
        System.out.println("3. Modern Java 21 Pattern Matching on Stream Events:");
        LLMEvent[] events = new LLMEvent[] {
            new ChunkEvent("Generative "),
            new ChunkEvent("AI "),
            new ChunkEvent("with "),
            new ChunkEvent("Java "),
            new ChunkEvent("is "),
            new ChunkEvent("blazing "),
            new ChunkEvent("fast!"),
            new FinishedEvent(142)
        };

        for (LLMEvent event : events) {
            handleStreamEvent(event);
        }
        System.out.println("==================================================");
    }
}
