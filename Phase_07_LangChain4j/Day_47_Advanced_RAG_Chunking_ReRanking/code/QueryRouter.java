package com.genai.langchain4j.advancedrag;

import java.util.Map;

/**
 * Enterprise Query Router dynamically directing questions to specialized vector databases.
 */
public class QueryRouter {

    public enum DestinationStore {
        INFRASTRUCTURE_DEV_DOCS,
        LEGAL_AND_HR_POLICY,
        FINANCIAL_ACCOUNTING
    }

    public DestinationStore route(String userQuery) {
        String lower = userQuery.toLowerCase();

        if (lower.contains("salary") || lower.contains("leave") || lower.contains("pto") || lower.contains("benefits") || lower.contains("hr") || lower.contains("policy")) {
            return DestinationStore.LEGAL_AND_HR_POLICY;
        }

        if (lower.contains("invoice") || lower.contains("billing") || lower.contains("tax") || lower.contains("refund") || lower.contains("payment")) {
            return DestinationStore.FINANCIAL_ACCOUNTING;
        }

        // Default to engineering/infrastructure documentation
        return DestinationStore.INFRASTRUCTURE_DEV_DOCS;
    }
}
