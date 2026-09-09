package com.genai.springai.structured;

import java.util.List;

public final class DomainRecords {

    private DomainRecords() {}

    public record FraudAssessment(
            boolean isFraudulent,
            double riskScore,
            List<String> anomalyFlags,
            String recommendedAction
    ) {}

    public record FinancialReport(
            String companyName,
            double revenueMillions,
            int fiscalYear,
            List<String> riskFactors
    ) {}
}
