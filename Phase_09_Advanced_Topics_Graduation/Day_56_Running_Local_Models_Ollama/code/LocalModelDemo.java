package com.genai.enterprise.localmodel;

/**
 * Verification test driver for running open-weight local models (Ollama) in Java 21.
 */
public class LocalModelDemo {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("   ENTERPRISE OFFLINE SOVEREIGN AI & LOCAL OLLAMA INFERENCE IN JAVA 21   ");
        System.out.println("==========================================================================\n");

        OllamaModelConfig config = OllamaModelConfig.defaultLlama32();
        OfflineAiService service = new OfflineAiService(config);

        System.out.println("[Step 1: Probing Local Ollama Daemon Status]");
        boolean reachable = service.getClient().isReachable();
        System.out.println("  Ollama Daemon Reachable at " + config.baseUrl() + ": " + (reachable ? "ONLINE" : "OFFLINE (Engaging Sovereign Simulation)"));
        System.out.println("  Configured Model: " + config.modelName() + " (Context: " + config.contextWindowTokens() + " tokens)\n");

        System.out.println("[Step 2: Processing Highly Confidential M&A Acquisition Document]");
        String docTitle = "Project Titan: Defense Merger Agreement 2026";
        String docBody = "Target enterprise holds top-secret avionics patents. Valuation: $4.8B. Strict zero-cloud confidentiality.";

        System.out.print("  Streaming Local LLM Tokens: ");
        var result = service.analyzeConfidentialDocument(docTitle, docBody, token -> {
            System.out.print(token);
        });
        System.out.println("\n");

        System.out.println("==========================================================================");
        System.out.println("                     SOVEREIGN INFERENCE AUDIT                            ");
        System.out.println("==========================================================================");
        System.out.println("Model Executed      : " + result.model());
        System.out.println("Latency             : " + result.latencyMs() + " ms");
        System.out.printf("Marginal Token Cost : $%.6f USD%n", result.costUsd());
        System.out.println("Data Egress Policy  : 100% AIR-GAPPED (Compliant: " + result.sovereignComplianceVerified() + ")");
        System.out.println("==========================================================================");
        System.out.println(">>> Local open-weight model verification completed successfully!");
    }
}
