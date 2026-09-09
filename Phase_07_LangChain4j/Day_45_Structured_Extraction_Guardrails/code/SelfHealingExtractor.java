package com.genai.langchain4j.extraction;

import java.util.List;

/**
 * Self-healing structured extractor with multi-turn error correction.
 */
public class SelfHealingExtractor {

    private final int maxRetries;

    public SelfHealingExtractor(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public ClinicalReport extractWithSelfHealing(String clinicalNotes, boolean simulateInitialFault) {
        // Step 1: Input Guardrail Screening
        InputGuardrail.GuardrailResult inputCheck = InputGuardrail.evaluate(clinicalNotes);
        if (!inputCheck.passed()) {
            throw new SecurityException("INPUT GUARDRAIL REJECTION: " + inputCheck.violationReason());
        }

        ClinicalReport currentReport = null;
        int attempts = 0;
        boolean hasFault = simulateInitialFault;

        while (attempts < maxRetries) {
            attempts++;
            System.out.printf("[Attempt %d/%d] Model generating structured extraction...\n", attempts, maxRetries);

            if (hasFault) {
                // Simulating an initial model output with missing citation (hallucination violation)
                System.out.println("   [Model Output] Emitted report missing verbatim citation!");
                currentReport = new ClinicalReport(
                    "Marcus Brody",
                    52,
                    "142/92",
                    88,
                    List.of("Severe chest tightness", "Shortness of breath"),
                    ClinicalReport.TriageLevel.URGENT,
                    "" // empty citation triggers guardrail violation
                );
                hasFault = false; // Resolved on next attempt
            } else {
                // Correct model output with citation
                currentReport = new ClinicalReport(
                    "Marcus Brody",
                    52,
                    "142/92",
                    88,
                    List.of("Severe chest tightness", "Shortness of breath"),
                    ClinicalReport.TriageLevel.URGENT,
                    "Patient Marcus Brody, 52 yo, presents with severe chest tightness and shortness of breath; BP recorded at 142/92, pulse 88 bpm."
                );
            }

            // Step 2: Output Guardrail Validation
            OutputGuardrail.ValidationResult outputCheck = OutputGuardrail.validateClinicalReport(currentReport);
            if (outputCheck.isValid()) {
                System.out.println("   [Output Guardrail] ✅ PASS: All clinical constraints and citations verified.");
                return currentReport;
            } else {
                System.out.println("   [Output Guardrail] ⚠️ FAIL: " + outputCheck.errorMessage());
                System.out.println("   [Self-Healing Loop] Appending error feedback to prompt for corrective retry...");
            }
        }

        throw new IllegalStateException("Failed to extract valid clinical report after " + maxRetries + " attempts.");
    }
}
