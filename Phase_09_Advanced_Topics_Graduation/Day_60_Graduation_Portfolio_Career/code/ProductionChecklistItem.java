package com.genai.enterprise.graduation;

/**
 * Item in the Enterprise Production Deployment Checklist.
 */
public record ProductionChecklistItem(
        String category,
        String checkName,
        String description,
        boolean passed,
        String remediationAdvice
) {
    public String getStatusTag() {
        return passed ? "[PASS]" : "[ACTION REQUIRED]";
    }
}
