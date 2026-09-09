package com.genai.langchain4j.react;

/**
 * Executable demonstration of Day 49:
 * Building a ReAct (Reasoning + Acting) Agent in Java.
 */
public class ReActAgentDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  DAY 49: REACT (REASONING + ACTING) AUTONOMOUS AGENT DEMO       ");
        System.out.println("==================================================================");

        OrderManagementTools tools = new OrderManagementTools();
        ReActAgentLoop agent = new ReActAgentLoop(tools, 6);

        // Scenario 1: Multi-Step Autonomous Goal Resolution
        System.out.println("\n--- SCENARIO 1: Autonomous Incident Triage & Resolution ---");
        String complexGoal = "Investigate shipping delay on order ORD-991, calculate delay from carrier, issue $50 credit if delay > 48h, and finalize report.";

        AgentExecutionResult result = agent.solveGoal(complexGoal, false);
        System.out.println("\n--- EXECUTION AUDIT SUMMARY ---");
        System.out.println("Goal:                " + result.goal());
        System.out.println("Status:              " + (result.completedSuccessfully() ? "✅ SUCCESS" : "❌ FAILED"));
        System.out.println("Stop Reason:         " + result.stopReason());
        System.out.println("Total Iterations:    " + result.iterations());
        System.out.println("Recorded Steps:      " + result.steps().size());

        // Scenario 2: Guardrail Stopping Condition (Loop Trap Detection)
        System.out.println("\n--- SCENARIO 2: Runaway Repetition Loop Trap Detection ---");
        String loopGoal = "Query the order status continuously until a new status is detected.";
        AgentExecutionResult loopResult = agent.solveGoal(loopGoal, true);

        System.out.println("\nLoop Trap Status:    " + loopResult.stopReason());
        System.out.println("Completed:           " + loopResult.completedSuccessfully());

        System.out.println("\n==================================================================");
        System.out.println("  REACT AGENT DEMO COMPLETED SUCCESSFULLY                        ");
        System.out.println("==================================================================");
    }
}
