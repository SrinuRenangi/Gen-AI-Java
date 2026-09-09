package com.genai.langchain4j.tools;

import java.util.Map;

/**
 * Executable demonstration of Day 48:
 * Tool Execution and Function Calling in LangChain4j.
 */
public class LangChain4jToolExecutionDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  DAY 48: LANGCHAIN4J TOOL EXECUTION & FUNCTION CALLING DEMO     ");
        System.out.println("==================================================================");

        // 1. Scan and Register Tools
        ToolRegistry registry = new ToolRegistry();
        registry.registerTools(new EnterpriseBusinessTools());

        System.out.println("\n--- 1. Registered Tool Specifications (Scanned via Reflection) ---");
        for (ToolSpecification spec : registry.getSpecifications()) {
            System.out.println("Tool Name: " + spec.name());
            System.out.println("   Description: " + spec.description());
            System.out.println("   Parameters:  " + spec.parameters());
        }

        // 2. Execute Financial Calculator Tool
        System.out.println("\n--- 2. Executing Financial Amortization Tool ---");
        ToolExecutionRequest calcReq = new ToolExecutionRequest(
            "call_calc_01",
            "calculateMonthlyPayment",
            Map.of("principal", 450000.0, "annualRatePercent", 6.5, "termMonths", 360)
        );

        ToolExecutionResultMessage calcRes = registry.execute(calcReq);
        System.out.println("Tool Request:  " + calcReq.name() + " -> " + calcReq.arguments());
        System.out.println("Tool Response: $" + calcRes.resultText() + " / month");

        // 3. Execute Inventory Stock Tool
        System.out.println("\n--- 3. Executing Inventory Stock Verification Tool ---");
        ToolExecutionRequest invReq = new ToolExecutionRequest(
            "call_inv_02",
            "checkInventoryStock",
            Map.of("sku", "SKU-990-PRO")
        );

        ToolExecutionResultMessage invRes = registry.execute(invReq);
        System.out.println("Tool Request:  " + invReq.name() + " -> " + invReq.arguments());
        System.out.println("Tool Response: " + invRes.resultText());

        // 4. Test Exception Containment & Quota Guardrail
        System.out.println("\n--- 4. Exception Containment on High-Risk Operation (Quota Exceeded) ---");
        ToolExecutionRequest gpuReq = new ToolExecutionRequest(
            "call_gpu_03",
            "reserveGpuInstances",
            Map.of("clusterId", "prod-us-east-1", "nodeCount", 16) // exceeds quota of 8!
        );

        ToolExecutionResultMessage gpuRes = registry.execute(gpuReq);
        System.out.println("Tool Request:  " + gpuReq.name() + " -> " + gpuReq.arguments());
        System.out.println("Tool Response: " + gpuRes.resultText());

        System.out.println("\n==================================================================");
        System.out.println("  LANGCHAIN4J TOOL EXECUTION VERIFICATION COMPLETED SUCCESSFULLY ");
        System.out.println("==================================================================");
    }
}
