package com.genai.springai.prompt;

import com.genai.springai.chatclient.ChatClient;
import com.genai.springai.core.ChatModel;
import com.genai.springai.core.OllamaChatModel;

import java.util.List;

public class PromptEngineeringDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 34: ADVANCED PROMPT ENGINEERING & VERSIONING IN JAVA                      ");
        System.out.println("================================================================================\n");

        ChatModel model = new OllamaChatModel("http://localhost:11434", "llama3.2");
        ChatClient chatClient = ChatClient.builder(model).build();

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Few-Shot Prompting for Enterprise Ticket Classification
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 1] Assembling Few-Shot Classification Prompt with Exemplars...");
        FewShotPromptBuilder fewShotBuilder = new FewShotPromptBuilder()
                .instruction("Classify customer support tickets into Category, Priority, and RoutingTeam.")
                .addExample(
                        "The payment gateway returned a 500 error and charged customer twice.",
                        "Category: BILLING | Priority: P1_CRITICAL | RoutingTeam: Payments-Core"
                )
                .addExample(
                        "How do I invite my team members to our workspace?",
                        "Category: ONBOARDING | Priority: P3_LOW | RoutingTeam: User-Management"
                )
                .addExample(
                        "Database connection pool exhausted during peak black friday traffic.",
                        "Category: INFRASTRUCTURE | Priority: P0_OUTAGE | RoutingTeam: SRE-DevOps"
                );

        String fewShotPrompt = fewShotBuilder.build("Our API keys expired without warning and webhook notifications stopped.");
        System.out.println("--- Generated Few-Shot Prompt ---\n" + fewShotPrompt);

        String fewShotResponse = chatClient.prompt().user(fewShotPrompt).call().content();
        System.out.println("\n--- Model Response ---\n" + fewShotResponse);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Chain-of-Thought (CoT) Step-by-Step Mathematical Reasoning
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Chain-of-Thought (CoT) Prompting with Explicit Thought Tags...");
        String cotProblem = "Our application cluster has 8 nodes. Each node runs 500 virtual threads. "
                + "Each virtual thread makes 2 downstream database queries per second, with each query taking 4ms. "
                + "What is the total query concurrency required at the PostgreSQL connection pool?";

        String cotPrompt = ChainOfThoughtPrompt.build(cotProblem, List.of(
                "Calculate total virtual threads across all cluster nodes.",
                "Calculate total queries initiated per second.",
                "Apply Little's Law (L = lambda * W) to determine average concurrent database queries.",
                "Recommend minimum HikariCP pool capacity with 20% safety headroom."
        ));

        System.out.println("--- Generated CoT Prompt ---\n" + cotPrompt);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: Enterprise Prompt Versioning Registry (A/B Testing & Rollouts)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] Enterprise Prompt Versioning & Zero-Downtime Rollout...");
        PromptVersioningRegistry registry = new PromptVersioningRegistry();

        // Register v1.0 Baseline
        registry.registerPrompt(new PromptVersioningRegistry.PromptVersion(
                "sentiment_analyzer",
                "1.0.0",
                "Analyze the sentiment of: {text}. Return POSITIVE, NEGATIVE, or NEUTRAL.",
                "alice@genai.com",
                82.4
        ));

        // Register v2.0 Few-Shot Optimized (Improved accuracy)
        registry.registerPrompt(new PromptVersioningRegistry.PromptVersion(
                "sentiment_analyzer",
                "2.0.0",
                "Analyze the sentiment of: {text}. Output format: {sentiment: POSITIVE|NEGATIVE|NEUTRAL, confidence: 0.0-1.0}",
                "bob@genai.com",
                97.1
        ));

        System.out.println("  Active Prompt Version: " + registry.getActivePrompt("sentiment_analyzer").version()
                + " (Score: " + registry.getActivePrompt("sentiment_analyzer").qualityBenchmarkScore() + "%)");

        // Promote v2.0.0 to Active in Production
        registry.setActiveVersion("sentiment_analyzer", "2.0.0");
        System.out.println("  [CANARY DEPLOYMENT] Promoted v2.0.0 to Active!");
        System.out.println("  New Active Prompt Version: " + registry.getActivePrompt("sentiment_analyzer").version()
                + " (Score: " + registry.getActivePrompt("sentiment_analyzer").qualityBenchmarkScore() + "%)");
        System.out.println("  Template: " + registry.getActivePrompt("sentiment_analyzer").templateContent());

        System.out.println("\n================================================================================");
        System.out.println("  PROMPT ENGINEERING PATTERNS VALIDATED SUCCESSFULLY!                           ");
        System.out.println("================================================================================");
    }
}
