package com.javagenai.day15;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PromptStore {
    private final Map<String, PromptTemplate> storage = new ConcurrentHashMap<>();

    public PromptStore() {
        // Seed default
        String id = "prompt-001";
        storage.put(id, new PromptTemplate(
            id, "Java Senior Code Reviewer", "Engineering",
            "Review the following Java code for memory leaks: {code}",
            0.2, Instant.now()
        ));
    }

    public List<PromptTemplate> findAll(String category) {
        if (category == null || category.isBlank()) {
            return new ArrayList<>(storage.values());
        }
        return storage.values().stream()
            .filter(p -> category.equalsIgnoreCase(p.category()))
            .toList();
    }

    public Optional<PromptTemplate> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    public PromptTemplate save(PromptTemplate prompt) {
        storage.put(prompt.id(), prompt);
        return prompt;
    }

    public boolean delete(String id) {
        return storage.remove(id) != null;
    }
}
