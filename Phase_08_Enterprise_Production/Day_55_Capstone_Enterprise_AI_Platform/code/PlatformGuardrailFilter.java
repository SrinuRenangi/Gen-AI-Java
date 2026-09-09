package com.genai.enterprise.capstone;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Enterprise guardrail filter performing prompt injection detection and PII redaction.
 */
public class PlatformGuardrailFilter {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore (?:all )?(?:previous|above) instructions"),
            Pattern.compile("(?i)disregard (?:system|developer) (?:prompt|rules)"),
            Pattern.compile("(?i)system override"),
            Pattern.compile("(?i)reveal (?:internal|system) (?:prompt|keys|credentials)")
    );

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    public record ScanResult(boolean passed, String sanitizedPrompt, String violationReason) {}

    public ScanResult scanPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return new ScanResult(false, "", "Prompt cannot be empty");
        }

        // 1. Check prompt injection
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(prompt).find()) {
                return new ScanResult(false, prompt, "SECURITY VIOLATION: Prompt injection signature detected: " + p.pattern());
            }
        }

        // 2. Redact PII in input
        String sanitized = SSN_PATTERN.matcher(prompt).replaceAll("[REDACTED_SSN]");
        sanitized = CREDIT_CARD_PATTERN.matcher(sanitized).replaceAll("[REDACTED_CARD]");

        return new ScanResult(true, sanitized, "PASSED");
    }

    public String sanitizeOutput(String output) {
        if (output == null) return null;
        String sanitized = SSN_PATTERN.matcher(output).replaceAll("[REDACTED_SSN]");
        return CREDIT_CARD_PATTERN.matcher(sanitized).replaceAll("[REDACTED_CARD]");
    }
}
