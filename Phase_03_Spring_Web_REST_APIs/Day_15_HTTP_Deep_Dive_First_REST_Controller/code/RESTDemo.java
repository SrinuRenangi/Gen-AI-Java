package com.javagenai.day15;

import java.time.Instant;
import java.util.*;

public class RESTDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 15: PROMPT LIBRARY REST API CRUD DEMO      ");
        System.out.println("==================================================");

        PromptStore store = new PromptStore();

        // 1. GET ALL PROMPTS
        System.out.println("1. HTTP GET /api/v1/prompts -> 200 OK");
        List<PromptTemplate> all = store.findAll(null);
        all.forEach(p -> System.out.printf("   [%s] %s (Cat: %s, Temp: %.1f)%n", p.id(), p.title(), p.category(), p.defaultTemperature()));
        System.out.println();

        // 2. CREATE (POST)
        System.out.println("2. HTTP POST /api/v1/prompts -> 201 CREATED");
        String newId = "prompt-002";
        PromptTemplate newPrompt = new PromptTemplate(
            newId, "RAG Context Synthesizer", "AI-Core",
            "Synthesize context: {context} to answer query: {query}",
            0.5, Instant.now()
        );
        store.save(newPrompt);
        System.out.printf("   Created: [%s] %s%n", newPrompt.id(), newPrompt.title());
        System.out.println();

        // 3. GET BY ID & RENDER VARIABLES
        System.out.println("3. HTTP GET /api/v1/prompts/prompt-002 -> 200 OK");
        Optional<PromptTemplate> fetched = store.findById("prompt-002");
        if (fetched.isPresent()) {
            PromptTemplate p = fetched.get();
            System.out.println("   Fetched template text: " + p.templateText());

            // Rendering prompt
            Map<String, String> variables = Map.of(
                "context", "PostgreSQL pgvector enables HNSW indexing.",
                "query", "How does vector index speed up search?"
            );
            String rendered = p.templateText();
            for (var entry : variables.entrySet()) {
                rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            System.out.println("   Rendered Prompt:\n   \"" + rendered + "\"");
        }
        System.out.println();

        // 4. DELETE
        System.out.println("4. HTTP DELETE /api/v1/prompts/prompt-001 -> 204 NO CONTENT");
        boolean deleted = store.delete("prompt-001");
        System.out.println("   Deletion successful? " + deleted);
        System.out.println("   Remaining prompts in store: " + store.findAll(null).size());
        System.out.println("==================================================");
    }
}
