package com.genai.enterprise.security;

import java.util.regex.Pattern;

/**
 * High-speed deterministic PII scrubbing utility redacting SSNs, credit cards, emails, and phone numbers.
 */
public class PiiScrubber {

    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CC_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?1[-. ]?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})\\b");

    public record ScrubResult(String scrubbedText, int redactedCount) {}

    public static ScrubResult scrub(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new ScrubResult("", 0);
        }

        int count = 0;
        String result = rawText;

        if (SSN_PATTERN.matcher(result).find()) {
            result = SSN_PATTERN.matcher(result).replaceAll("[REDACTED_SSN]");
            count++;
        }
        if (CC_PATTERN.matcher(result).find()) {
            result = CC_PATTERN.matcher(result).replaceAll("[REDACTED_CREDIT_CARD]");
            count++;
        }
        if (EMAIL_PATTERN.matcher(result).find()) {
            result = EMAIL_PATTERN.matcher(result).replaceAll("[REDACTED_EMAIL]");
            count++;
        }
        if (PHONE_PATTERN.matcher(result).find()) {
            result = PHONE_PATTERN.matcher(result).replaceAll("[REDACTED_PHONE]");
            count++;
        }

        return new ScrubResult(result, count);
    }
}
