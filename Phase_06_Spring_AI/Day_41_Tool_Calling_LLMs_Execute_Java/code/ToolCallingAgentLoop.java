package com.genai.springai.tools;

import java.util.*;

/**
 * Simulates the Spring AI / OpenAI function calling orchestration loop.
 * Demonstrates:
 * 1. Schema injection into prompt
 * 2. Model emitting structured tool_call intents
 * 3. Java runtime executing business logic methods
 * 4. Tool outputs fed back to Model
 * 5. Final synthesis for user
 */
public class ToolCallingAgentLoop {

    public record ToolCall(String id, String functionName, Map<String, Object> arguments) {}
    public record AgentStep(String role, String content, List<ToolCall> toolCalls) {}

    private final ToolRegistry registry;
    private final List<AgentStep> conversationHistory = new ArrayList<>();

    public ToolCallingAgentLoop(ToolRegistry registry) {
        this.registry = registry;
    }

    public List<AgentStep> getHistory() {
        return Collections.unmodifiableList(conversationHistory);
    }

    public String run(String userPrompt) {
        conversationHistory.add(new AgentStep("USER", userPrompt, List.of()));
        System.out.println("\n[USER PROMPT] " + userPrompt);

        // Turn 1: Model evaluates prompt against registered tools
        List<ToolCall> plannedCalls = planToolCalls(userPrompt);

        if (plannedCalls.isEmpty()) {
            String directAnswer = "I have sufficient general knowledge to answer: " + userPrompt;
            conversationHistory.add(new AgentStep("ASSISTANT", directAnswer, List.of()));
            return directAnswer;
        }

        // Model generated tool call requests
        System.out.println("[MODEL] Requesting tool execution (" + plannedCalls.size() + " calls planned):");
        for (ToolCall tc : plannedCalls) {
            System.out.println("   -> Invoke: " + tc.functionName() + " with args " + tc.arguments());
        }
        conversationHistory.add(new AgentStep("ASSISTANT", "Invoking external enterprise tools...", plannedCalls));

        // Framework executes the Java methods
        Map<String, String> toolResults = new LinkedHashMap<>();
        for (ToolCall tc : plannedCalls) {
            String result = registry.execute(tc.functionName(), tc.arguments());
            toolResults.put(tc.id(), result);
            System.out.println("[TOOL RESULT for " + tc.functionName() + "] " + result);
            conversationHistory.add(new AgentStep("TOOL", "Result of " + tc.functionName() + ": " + result, List.of()));
        }

        // Turn 2: Synthesize final answer using tool observations
        String finalAnswer = synthesizeFinalResponse(userPrompt, plannedCalls, toolResults);
        conversationHistory.add(new AgentStep("ASSISTANT", finalAnswer, List.of()));
        System.out.println("[FINAL ANSWER]\n" + finalAnswer);

        return finalAnswer;
    }

    private List<ToolCall> planToolCalls(String prompt) {
        String lower = prompt.toLowerCase();
        List<ToolCall> calls = new ArrayList<>();

        if (lower.contains("weather")) {
            if (lower.contains("london") && (lower.contains("san francisco") || lower.contains("sf"))) {
                // Parallel tool calling
                calls.add(new ToolCall("call_wx_01", "getCurrentWeather", Map.of("location", "London, UK", "unit", "celsius")));
                calls.add(new ToolCall("call_wx_02", "getCurrentWeather", Map.of("location", "San Francisco, CA", "unit", "celsius")));
            } else if (lower.contains("tokyo")) {
                calls.add(new ToolCall("call_wx_03", "getCurrentWeather", Map.of("location", "Tokyo, Japan", "unit", "celsius")));
            } else if (lower.contains("london")) {
                calls.add(new ToolCall("call_wx_04", "getCurrentWeather", Map.of("location", "London, UK", "unit", "celsius")));
            }
        } else if (lower.contains("transfer") || lower.contains("send money") || lower.contains("wire")) {
            double amount = 1500.00;
            if (lower.contains("15000") || lower.contains("15,000")) {
                amount = 15000.00;
            } else if (lower.contains("1500") || lower.contains("1,500")) {
                amount = 1500.00;
            }
            calls.add(new ToolCall("call_wire_01", "transferFunds", Map.of(
                "fromAccount", "ACCT-USA-9988",
                "toAccount", "ACCT-EUR-4411",
                "amount", amount
            )));
        } else if (lower.contains("database") || lower.contains("sql") || lower.contains("products") || lower.contains("orders")) {
            String sql = lower.contains("orders") ? "SELECT * FROM orders;" : "SELECT * FROM products;";
            calls.add(new ToolCall("call_sql_01", "executeReadOnlySql", Map.of("query", sql)));
        }

        return calls;
    }

    private String synthesizeFinalResponse(String prompt, List<ToolCall> calls, Map<String, String> results) {
        StringBuilder sb = new StringBuilder();
        if (calls.size() > 1) {
            sb.append("Based on live meteorological telemetry across multiple locations:\n");
            for (ToolCall tc : calls) {
                String loc = (String) tc.arguments().get("location");
                String res = results.get(tc.id());
                sb.append(" • ").append(loc).append(": ").append(res).append("\n");
            }
        } else if (!calls.isEmpty()) {
            ToolCall tc = calls.get(0);
            String res = results.get(tc.id());
            if (tc.functionName().equals("transferFunds")) {
                if (res.contains("REQUIRES_APPROVAL")) {
                    sb.append("⚠️ Transaction Held: The requested transfer exceeds our real-time auto-approval threshold ($5,000). A compliance token has been dispatched for manager authorization.");
                } else {
                    sb.append("✅ Wire Transfer Completed: Funds have been successfully routed via the automated settlement network.\nDetails: ").append(res);
                }
            } else if (tc.functionName().equals("getCurrentWeather")) {
                sb.append("Live Weather Report: ").append(res);
            } else if (tc.functionName().equals("executeReadOnlySql")) {
                sb.append("Database Query Results:\n").append(res);
            }
        }
        return sb.toString();
    }
}
