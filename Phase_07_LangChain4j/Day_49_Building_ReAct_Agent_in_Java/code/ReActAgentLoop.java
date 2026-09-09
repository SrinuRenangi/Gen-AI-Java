package com.genai.langchain4j.react;

import java.util.*;

/**
 * Pure Java implementation of the ReAct (Reasoning + Acting) autonomous agent loop.
 * Features:
 * - Multi-turn Thought -> Action -> Observation cycle
 * - Maximum iteration safeguard against runaway loops
 * - Loop trap detector identifying repeated identical actions
 * - Autonomous goal termination detection
 */
public class ReActAgentLoop {

    private final OrderManagementTools tools;
    private final int maxIterations;

    public ReActAgentLoop(OrderManagementTools tools, int maxIterations) {
        this.tools = tools;
        this.maxIterations = maxIterations;
    }

    public AgentExecutionResult solveGoal(String goal, boolean simulateInfiniteLoop) {
        List<AgentStep> stepHistory = new ArrayList<>();
        int iteration = 0;
        String lastActionSignature = "";
        int repeatedActionCount = 0;

        System.out.println("\n[AGENT GOAL]: " + goal);

        while (iteration < maxIterations) {
            iteration++;
            System.out.printf("\n--- ITERATION %d/%d ---\n", iteration, maxIterations);

            // Step 1: REASONING (Thought)
            String thought;
            AgentAction action;

            if (simulateInfiniteLoop) {
                thought = "I need to check the inventory database.";
                action = new AgentAction("fetchOrder", Map.of("orderId", "ORD-LOOP"));
            } else {
                // Realistic multi-step decision logic based on previous observations
                if (stepHistory.isEmpty()) {
                    thought = "To resolve the complaint for order ORD-991, I first need to retrieve the order record to find the tracking number and customer ID.";
                    action = new AgentAction("fetchOrder", Map.of("orderId", "ORD-991"));
                } else if (stepHistory.size() == 1) {
                    thought = "The order has tracking number TRK-7712. Now I must query the carrier telemetry to determine if the shipment was delayed and by how many hours.";
                    action = new AgentAction("checkCarrierTracking", Map.of("trackingNumber", "TRK-7712"));
                } else if (stepHistory.size() == 2) {
                    thought = "Carrier telemetry confirms the package was delayed by 72 hours due to a blizzard. Since the delay exceeds the 48-hour threshold, corporate policy dictates issuing a $50 courtesy credit to customer CUST-881.";
                    action = new AgentAction("issueWalletCredit", Map.of("customerId", "CUST-881", "amount", 50.0));
                } else {
                    thought = "All investigative steps and remedial credits have been completed. I can now synthesize the final resolution memorandum for the customer.";
                    action = AgentAction.FINISH;
                }
            }

            System.out.println("THOUGHT:     " + thought);

            // Check for goal completion
            if (action.isFinish()) {
                String finalAnswer = """
                    Investigation Complete:
                    1. Order ORD-991 was dispatched on time but experienced a 72-hour delivery delay via FedEx due to a blizzard hub closure.
                    2. Per our SLA commitment, customer account CUST-881 has been issued an automatic $50.00 wallet credit (Transaction: TXN-CREDIT-404).
                    3. Case marked as RESOLVED.
                    """;
                System.out.println("FINAL ANSWER:\n" + finalAnswer);
                return new AgentExecutionResult(goal, finalAnswer, stepHistory, iteration, true, "GOAL_ACHIEVED");
            }

            // Step 2: ACTION GUARD - Detect runaway repetition loops
            String currentSignature = action.toolName() + ":" + action.arguments();
            if (currentSignature.equals(lastActionSignature)) {
                repeatedActionCount++;
                if (repeatedActionCount >= 2) {
                    System.out.println("⚠️ LOOP TRAP DETECTED: Agent executed identical action '" + currentSignature + "' multiple times! Halting execution.");
                    return new AgentExecutionResult(goal, "Aborted due to loop trap.", stepHistory, iteration, false, "LOOP_TRAP_DETECTED");
                }
            } else {
                repeatedActionCount = 0;
            }
            lastActionSignature = currentSignature;

            System.out.println("ACTION:      " + action.toolName() + " with args " + action.arguments());

            // Step 3: OBSERVATION (Execute Java Tool)
            String observation;
            try {
                observation = switch (action.toolName()) {
                    case "fetchOrder" -> tools.fetchOrder((String) action.arguments().get("orderId"));
                    case "checkCarrierTracking" -> tools.checkCarrierTracking((String) action.arguments().get("trackingNumber"));
                    case "issueWalletCredit" -> tools.issueWalletCredit(
                        (String) action.arguments().get("customerId"),
                        ((Number) action.arguments().get("amount")).doubleValue()
                    );
                    default -> "{\"error\": \"Unknown tool: " + action.toolName() + "\"}";
                };
            } catch (Exception ex) {
                observation = "{\"error\": \"" + ex.getMessage() + "\"}";
            }

            System.out.println("OBSERVATION: " + observation);
            stepHistory.add(new AgentStep(iteration, thought, action, observation));
        }

        System.out.println("⚠️ MAX ITERATIONS REACHED: Agent exceeded budget of " + maxIterations + " turns.");
        return new AgentExecutionResult(goal, "Execution timed out.", stepHistory, iteration, false, "MAX_ITERATIONS_EXCEEDED");
    }
}
