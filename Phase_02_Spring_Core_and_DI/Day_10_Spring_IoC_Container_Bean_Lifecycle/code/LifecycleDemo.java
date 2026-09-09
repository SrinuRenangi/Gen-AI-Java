package com.javagenai.day10;

public class LifecycleDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 10: SPRING BEAN LIFECYCLE & SCOPES DEMO    ");
        System.out.println("==================================================");

        // 1. Simulating Initialization (@PostConstruct)
        System.out.println("1. Bean Initialization Phase (@PostConstruct):");
        LocalEmbeddingEngine engine = new LocalEmbeddingEngine();
        engine.initWarmup();
        System.out.println();

        // 2. Demonstrating Scopes: Singleton vs Prototype
        System.out.println("2. Scopes in Action: Singleton vs Prototype:");
        // Prototype behavior: Every user gets their OWN conversation session
        ChatConversationSession sessionUser1 = new ChatConversationSession();
        ChatConversationSession sessionUser2 = new ChatConversationSession();

        sessionUser1.addMessage("User: What is RAG?");
        sessionUser1.addMessage("Bot: Retrieval Augmented Generation.");

        sessionUser2.addMessage("User: What is Python?");

        System.out.println("   Session 1 History: " + sessionUser1.getHistory());
        System.out.println("   Session 2 History: " + sessionUser2.getHistory());
        System.out.println("   Are sessions independent? " + (sessionUser1 != sessionUser2));
        System.out.println();

        // 3. Simulating Shutdown (@PreDestroy)
        System.out.println("3. Bean Destruction Phase (@PreDestroy):");
        VectorDatabaseConnector connector = new VectorDatabaseConnector();
        connector.cleanup();
        System.out.println("==================================================");
    }
}
