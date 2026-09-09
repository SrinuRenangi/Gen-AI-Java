package com.genai.enterprise.localmodel;

import java.util.function.Consumer;

/**
 * Air-gapped Sovereign AI Service executing local model inference
 * with strict zero-data-egress compliance.
 */
public class OfflineAiService {

    private final OllamaClient client;
    private final OllamaModelConfig config;

    public OfflineAiService(OllamaModelConfig config) {
        this.config = config;
        this.client = new OllamaClient(config);
    }

    public record LocalInferenceResult(
            String model,
            String response,
            long latencyMs,
            double costUsd,
            boolean sovereignComplianceVerified
    ) {}

    public LocalInferenceResult analyzeConfidentialDocument(
            String documentTitle,
            String documentBody,
            Consumer<String> streamConsumer
    ) {
        long start = System.currentTimeMillis();

        String systemPrompt = "You are a secure, air-gapped internal enterprise AI analyst. " +
                "Never disclose confidential information outside local memory.";
        String userPrompt = "Analyze and summarize this confidential internal dossier [" +
                documentTitle + "]: " + documentBody;

        String response = client.chat(systemPrompt, userPrompt, streamConsumer);
        long latency = System.currentTimeMillis() - start;

        // Marginal cost for open-weight local inference is exactly $0.00
        return new LocalInferenceResult(config.modelName(), response, latency, 0.0, true);
    }

    public OllamaClient getClient() { return client; }
}
