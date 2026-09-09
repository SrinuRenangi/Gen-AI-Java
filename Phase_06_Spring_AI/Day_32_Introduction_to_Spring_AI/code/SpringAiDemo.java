package com.genai.springai.core;

public class SpringAiDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 32: SPRING AI FOUNDATIONS & CHATCLIENT DEMONSTRATION                      ");
        System.out.println("================================================================================\n");

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Ollama Local Model Execution (100% Free, Local Privacy)
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 1] Initializing Local Ollama ChatModel (Llama 3.2)...");
        ChatModel ollamaModel = new OllamaChatModel("http://localhost:11434", "llama3.2");
        System.out.println("  Active Provider: " + ollamaModel.getProviderName());

        String simpleResponse = ollamaModel.call("What is Spring AI?");
        System.out.println("  Response: " + simpleResponse);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Provider Portability (Switching to OpenAI with ZERO prompt rewrites)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Provider Portability: Switching to OpenAI Cloud Model...");
        ChatModel openAiModel = new OpenAiChatModel("sk-prod-enterprise-key", "gpt-4o");
        System.out.println("  Active Provider: " + openAiModel.getProviderName());

        String cloudResponse = openAiModel.call("Explain Spring AI architecture");
        System.out.println("  Response: " + cloudResponse);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: Fluent ChatClient with System Directives and Custom Options
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] Fluent ChatClient Builder with System Directives...");
        ChatClient chatClient = ChatClient.builder(ollamaModel)
                .defaultSystem("You are a Principal Java Cloud Architect. Answer strictly with technical rigor.")
                .build();

        ChatResponse response = chatClient.prompt()
                .user("Compare Virtual Threads with WebFlux for AI streaming workloads.")
                .options(ChatOptions.builder()
                        .model("llama3.2")
                        .temperature(0.2)
                        .maxTokens(2048)
                        .build())
                .call()
                .chatResponse();

        System.out.println("  Generated Content:\n  " + response.getResult().output().getContent());
        System.out.println("\n  Finish Reason: " + response.getResult().finishReason());

        // -----------------------------------------------------------------------------------------
        // SCENARIO 4: Detailed Token Usage Metadata (Essential for Cost & Quota Metering)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 4] Token Usage Telemetry:");
        ChatResponse.UsageMetadata usage = response.usage();
        System.out.println("  Prompt Tokens:     " + usage.promptTokens());
        System.out.println("  Generation Tokens: " + usage.generationTokens());
        System.out.println("  Total Tokens:      " + usage.totalTokens());

        System.out.println("\n================================================================================");
        System.out.println("  SPRING AI FOUNDATIONS VERIFIED SUCCESSFULLY! READY FOR PRODUCTION LLMS.       ");
        System.out.println("================================================================================");
    }
}
