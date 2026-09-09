package com.genai.springai.structured;

import java.util.List;

public class StructuredOutputDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 35: STRUCTURED OUTPUT — CONVERTING LLM RESPONSES TO JAVA OBJECTS          ");
        System.out.println("================================================================================\n");

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Generating JSON Schema & Prompt Directives for FraudAssessment
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 1] Generating JSON Schema for Domain Record: FraudAssessment...");
        BeanOutputConverter<DomainRecords.FraudAssessment> fraudConverter = 
                new BeanOutputConverter<>(DomainRecords.FraudAssessment.class);

        String formatInstructions = fraudConverter.getFormat();
        System.out.println("--- Generated Prompt Directives ---\n" + formatInstructions);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Parsing Messy LLM Output with Markdown Fences and Conversational Chatter
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Parsing Messy Raw LLM Output with Markdown Code Blocks...");
        String rawLlmResponse = """
                Sure! Here is the JSON response you requested for the transaction:
                ```json
                {
                  "isFraudulent": true,
                  "riskScore": 0.89,
                  "anomalyFlags": ["IP_GEOLOCATION_MISMATCH", "UNUSUAL_MIDNIGHT_AMOUNT", "VELOCITY_SPIKE"],
                  "recommendedAction": "BLOCK_TRANSACTION_AND_ALERT_CARDHOLDER"
                }
                ```
                Let me know if you need any additional compliance analysis!
                """;

        System.out.println("--- Raw LLM Text Received ---\n" + rawLlmResponse);

        DomainRecords.FraudAssessment assessment = fraudConverter.convert(rawLlmResponse);
        System.out.println("\n  ✅ SUCCESSFULLY DESERIALIZED INTO JAVA RECORD!");
        System.out.println("     Fraudulent:     " + assessment.isFraudulent());
        System.out.println("     Risk Score:     " + assessment.riskScore());
        System.out.println("     Anomaly Flags:  " + assessment.anomalyFlags());
        System.out.println("     Recommendation: " + assessment.recommendedAction());

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: Converting FinancialReport Record
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] Converting Complex FinancialReport Record...");
        BeanOutputConverter<DomainRecords.FinancialReport> reportConverter = 
                new BeanOutputConverter<>(DomainRecords.FinancialReport.class);

        String financialJson = """
                {
                  "companyName": "Acme Cloud AI Inc.",
                  "revenueMillions": 348.5,
                  "fiscalYear": 2026,
                  "riskFactors": ["GPU_SUPPLY_CHAIN", "CURRENCY_FLUCTUATION", "REGULATORY_COMPLIANCE"]
                }
                """;

        DomainRecords.FinancialReport report = reportConverter.convert(financialJson);
        System.out.println("  ✅ SUCCESSFULLY DESERIALIZED FINANCIAL REPORT!");
        System.out.println("     Company:       " + report.companyName());
        System.out.println("     Revenue ($M):  $" + report.revenueMillions());
        System.out.println("     Fiscal Year:   " + report.fiscalYear());
        System.out.println("     Risk Factors:  " + report.riskFactors());

        System.out.println("\n================================================================================");
        System.out.println("  STRUCTURED OUTPUT CONVERSION VALIDATED WITH ZERO RUNTIME ERRORS!              ");
        System.out.println("================================================================================");
    }
}
