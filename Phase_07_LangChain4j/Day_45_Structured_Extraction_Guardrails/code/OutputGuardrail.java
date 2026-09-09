package com.genai.langchain4j.extraction;

/**
 * Output guardrail verifying domain constraints and hallucination defense.
 */
public class OutputGuardrail {

    public record ValidationResult(boolean isValid, String errorMessage) {}

    public static ValidationResult validateClinicalReport(ClinicalReport report) {
        if (report.patientName() == null || report.patientName().isBlank()) {
            return new ValidationResult(false, "Patient legal name cannot be null or empty.");
        }

        if (report.age() < 0 || report.age() > 125) {
            return new ValidationResult(false, "Age " + report.age() + " is biologically invalid.");
        }

        if (report.heartRateBpm() < 30 || report.heartRateBpm() > 250) {
            return new ValidationResult(false, "Heart rate " + report.heartRateBpm() + " bpm is outside clinical survival envelope.");
        }

        if (report.verbatimCitation() == null || report.verbatimCitation().isBlank()) {
            return new ValidationResult(false, "HALLUCINATION DEFENSE VIOLATION: Source citation quote is missing.");
        }

        if (report.detectedSymptoms() == null || report.detectedSymptoms().isEmpty()) {
            return new ValidationResult(false, "At least one detected symptom must be present in report.");
        }

        return new ValidationResult(true, "OK");
    }
}
