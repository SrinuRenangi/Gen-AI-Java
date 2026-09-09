package com.genai.springai.tools;

/**
 * Executable demonstration of Day 41:
 * Tool Calling - LLMs executing Java methods.
 */
public class ToolCallingDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  DAY 41: SPRING AI TOOL CALLING & JAVA METHOD EXECUTION DEMO    ");
        System.out.println("==================================================================");

        // 1. Initialize Registry and register Java tools
        ToolRegistry registry = new ToolRegistry();
        registry.register(new WeatherTool())
                .register(new AccountTransferTool())
                .register(new SqlExecutorTool());

        System.out.println("\n[1] Registered Tool Schemas Exported for Model System Prompt:");
        System.out.println(registry.exportJsonSchemas());

        ToolCallingAgentLoop agent = new ToolCallingAgentLoop(registry);

        // Scenario 1: Single Tool Call (Live Weather)
        System.out.println("\n--- SCENARIO 1: Single Tool Invocation ---");
        agent.run("What is the current weather in Tokyo right now?");

        // Scenario 2: Parallel Tool Calling (Compare Weather in London and San Francisco)
        System.out.println("\n--- SCENARIO 2: Parallel Tool Invocations ---");
        agent.run("Can you compare the current weather between London and San Francisco?");

        // Scenario 3A: Standard Business Action within Guardrail Limits
        System.out.println("\n--- SCENARIO 3A: Automated Action within Guardrail Limits ---");
        agent.run("Please transfer $1,500 from ACCT-USA-9988 to ACCT-EUR-4411.");

        // Scenario 3B: High-Risk Action Triggering Human-in-the-Loop Safeguard
        System.out.println("\n--- SCENARIO 3B: High-Risk Action Triggering Human-in-the-Loop ---");
        agent.run("Please wire $15,000 from ACCT-USA-9988 to ACCT-EUR-4411 immediately.");

        // Scenario 4: Secure Data Warehouse Querying
        System.out.println("\n--- SCENARIO 4: Enterprise SQL Database Querying ---");
        agent.run("Check the database for our top products catalog and remaining stock.");

        System.out.println("\n==================================================================");
        System.out.println("  TOOL CALLING VERIFICATION COMPLETED SUCCESSFULLY               ");
        System.out.println("==================================================================");
    }
}
