package com.javagenai.day02;

import java.util.HashMap;
import java.util.Map;

public class MemoryAndEqualityDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("      DAY 02: OOP & MEMORY INTERACTION DEMO       ");
        System.out.println("==================================================");

        // 1. Classes & Encapsulation Demo
        ChatMessage userMsg = new ChatMessage("user", "Explain how embeddings work in pgvector.");
        System.out.println("1. ChatMessage Encapsulation:");
        System.out.println("   " + userMsg);
        System.out.println("   Is System Message? " + userMsg.isSystemMessage());
        System.out.println();

        // 2. Vector Math Demo
        Vector3D vecA = new Vector3D(1.0, 2.0, 3.0);
        Vector3D vecB = new Vector3D(2.0, 4.0, 6.0); // Exact same direction (parallel)
        Vector3D vecC = new Vector3D(-1.0, -2.0, 3.0);

        System.out.println("2. 3D Vector Operations:");
        System.out.printf("   Vec A: %s, Mag: %.4f%n", vecA, vecA.magnitude());
        System.out.printf("   Vec B: %s, Mag: %.4f%n", vecB, vecB.magnitude());
        System.out.printf("   Cosine Similarity (A, B - identical direction): %.4f%n", vecA.cosineSimilarity(vecB));
        System.out.printf("   Cosine Similarity (A, C - orthogonal/divergent): %.4f%n", vecA.cosineSimilarity(vecC));
        System.out.println();

        // 3. equals() and hashCode() Contract Demo
        System.out.println("3. The equals() & hashCode() Contract in Action:");
        AIModelSpecification spec1 = new AIModelSpecification("gpt-4o-mini", 128000, 0.15);
        AIModelSpecification spec2 = new AIModelSpecification("GPT-4O-MINI", 128000, 0.15); // Case-insensitive match

        System.out.println("   Spec 1: " + spec1);
        System.out.println("   Spec 2: " + spec2);
        System.out.println("   spec1 == spec2      : " + (spec1 == spec2) + " (Distinct heap memory addresses)");
        System.out.println("   spec1.equals(spec2) : " + spec1.equals(spec2) + " (Logical identity match)");
        System.out.println("   spec1.hashCode()    : " + spec1.hashCode());
        System.out.println("   spec2.hashCode()    : " + spec2.hashCode() + " (Identical hashcodes guaranteed!)");

        // Storing in a Map
        Map<AIModelSpecification, String> providerMap = new HashMap<>();
        providerMap.put(spec1, "OpenAI Primary Gateway");

        // Retrieving using spec2
        String retrieved = providerMap.get(spec2);
        System.out.println("   HashMap lookup by equivalent key: " + retrieved);
        System.out.printf("   Cost for 500,000 tokens: $%.4f USD%n", spec1.calculateInferenceCost(500000));
        System.out.println("==================================================");
    }
}
