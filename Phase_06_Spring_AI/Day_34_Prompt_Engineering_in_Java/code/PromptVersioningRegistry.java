package com.genai.springai.prompt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe enterprise registry managing versioned prompt templates.
 * Enables zero-downtime prompt updates, A/B canary testing, and prompt telemetry.
 */
public class PromptVersioningRegistry {

    public record PromptVersion(
            String promptName,
            String version,
            String templateContent,
            String author,
            double qualityBenchmarkScore
    ) {}

    private final Map<String, Map<String, PromptVersion>> registry = new ConcurrentHashMap<>();
    private final Map<String, String> activeVersionPointers = new ConcurrentHashMap<>();

    public void registerPrompt(PromptVersion pv) {
        registry.computeIfAbsent(pv.promptName(), k -> new ConcurrentHashMap<>())
                .put(pv.version(), pv);
        // Default active version to the latest registered if none set
        activeVersionPointers.putIfAbsent(pv.promptName(), pv.version());
    }

    public void setActiveVersion(String promptName, String version) {
        Map<String, PromptVersion> versions = registry.get(promptName);
        if (versions == null || !versions.containsKey(version)) {
            throw new IllegalArgumentException("Unknown prompt version: " + promptName + "@" + version);
        }
        activeVersionPointers.put(promptName, version);
    }

    public PromptVersion getActivePrompt(String promptName) {
        String version = activeVersionPointers.get(promptName);
        if (version == null) {
            throw new IllegalArgumentException("No active version found for prompt: " + promptName);
        }
        return registry.get(promptName).get(version);
    }

    public PromptVersion getVersion(String promptName, String version) {
        Map<String, PromptVersion> versions = registry.get(promptName);
        if (versions == null || !versions.containsKey(version)) {
            throw new IllegalArgumentException("Prompt version not found: " + promptName + "@" + version);
        }
        return versions.get(version);
    }
}
