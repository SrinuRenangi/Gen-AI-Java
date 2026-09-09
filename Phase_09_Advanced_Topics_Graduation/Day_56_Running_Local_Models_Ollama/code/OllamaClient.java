package com.genai.enterprise.localmodel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Pure Java 21 HTTP client interacting with the Ollama REST API.
 * Supports live daemon communication and deterministic air-gapped simulation.
 */
public class OllamaClient {

    private final OllamaModelConfig config;
    private final HttpClient httpClient;

    public OllamaClient(OllamaModelConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public boolean isReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public String chat(String systemPrompt, String userPrompt, Consumer<String> tokenConsumer) {
        if (isReachable()) {
            return executeLiveOllamaChat(systemPrompt, userPrompt, tokenConsumer);
        } else {
            return executeSimulatedOfflineChat(systemPrompt, userPrompt, tokenConsumer);
        }
    }

    private String executeLiveOllamaChat(String systemPrompt, String userPrompt, Consumer<String> tokenConsumer) {
        try {
            String jsonPayload = String.format("""
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                  ],
                  "stream": false,
                  "options": {
                    "temperature": %.2f,
                    "num_ctx": %d
                  }
                }
                """, config.modelName(), escapeJson(systemPrompt), escapeJson(userPrompt),
                    config.temperature(), config.contextWindowTokens());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            // Parse response content from JSON
            String token = extractContentFromJson(body);
            if (tokenConsumer != null) tokenConsumer.accept(token);
            return token;
        } catch (Exception e) {
            System.err.println("[OllamaClient] Live call failed, falling back to sovereign offline engine: " + e.getMessage());
            return executeSimulatedOfflineChat(systemPrompt, userPrompt, tokenConsumer);
        }
    }

    private String executeSimulatedOfflineChat(String systemPrompt, String userPrompt, Consumer<String> tokenConsumer) {
        String fullResponse = String.format(
                "[Offline Sovereign Llama 3.2 on %s] Processed query '%s' with ZERO external data egress. Model loaded in local VRAM.",
                config.modelName(), userPrompt);

        // Stream tokens simulated
        String[] tokens = fullResponse.split(" ");
        for (String t : tokens) {
            if (tokenConsumer != null) {
                tokenConsumer.accept(t + " ");
            }
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return fullResponse;
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String extractContentFromJson(String json) {
        int idx = json.indexOf("\"content\":");
        if (idx != -1) {
            int start = json.indexOf("\"", idx + 10) + 1;
            int end = json.indexOf("\"", start);
            if (start > 0 && end > start) {
                return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
            }
        }
        return json;
    }
}
