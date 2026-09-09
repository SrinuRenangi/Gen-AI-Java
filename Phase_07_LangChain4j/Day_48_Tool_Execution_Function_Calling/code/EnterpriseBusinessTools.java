package com.genai.langchain4j.tools;

/**
 * Enterprise service exposing business methods annotated with @Tool and @P.
 */
public class EnterpriseBusinessTools {

    @Tool("Calculates monthly mortgage or loan payment given principal, annual interest rate percentage, and term in months")
    public double calculateMonthlyPayment(
        @P("Loan principal amount in USD") double principal,
        @P("Annual interest rate percentage, e.g. 6.5 for 6.5%") double annualRatePercent,
        @P("Loan term in months, e.g. 360 for 30-year fixed") int termMonths
    ) {
        if (principal <= 0 || termMonths <= 0) {
            throw new IllegalArgumentException("Principal and term months must be strictly positive.");
        }
        double monthlyRate = (annualRatePercent / 100.0) / 12.0;
        if (monthlyRate == 0) return principal / termMonths;
        double payment = (principal * monthlyRate * Math.pow(1 + monthlyRate, termMonths)) / (Math.pow(1 + monthlyRate, termMonths) - 1);
        return Math.round(payment * 100.0) / 100.0;
    }

    @Tool("Checks inventory level and warehouse distribution center for an enterprise SKU")
    public String checkInventoryStock(@P("Product SKU, e.g. SKU-100-PRO") String sku) {
        if (sku.toUpperCase().contains("PRO")) {
            return "{\"sku\": \"" + sku + "\", \"inStock\": 42, \"warehouse\": \"Dallas-DC-1\", \"status\": \"AVAILABLE\"}";
        } else {
            return "{\"sku\": \"" + sku + "\", \"inStock\": 0, \"warehouse\": \"Chicago-DC-2\", \"status\": \"BACKORDERED\"}";
        }
    }

    @Tool("Provisions dedicated GPU cloud instances for enterprise training clusters")
    public String reserveGpuInstances(
        @P("Cluster identifier") String clusterId,
        @P("Number of GPU nodes to allocate") int nodeCount
    ) {
        if (nodeCount > 8) {
            throw new SecurityException("Quota exceeded: Maximum autonomous allocation without VP sign-off is 8 GPU nodes.");
        }
        return "{\"status\": \"RESERVED\", \"clusterId\": \"" + clusterId + "\", \"nodesAllocated\": " + nodeCount + ", \"gpuType\": \"NVIDIA H100 SXM5\"}";
    }
}
