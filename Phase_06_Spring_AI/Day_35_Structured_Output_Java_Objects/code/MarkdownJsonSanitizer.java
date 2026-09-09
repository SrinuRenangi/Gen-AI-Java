package com.genai.springai.structured;

/**
 * Resilient utility that strips conversational filler and markdown code fences:
 * ```json
 * { ... }
 * ```
 * from LLM completions, leaving pure JSON text ready for deserialization.
 */
public class MarkdownJsonSanitizer {

    public static String clean(String rawLlmOutput) {
        if (rawLlmOutput == null || rawLlmOutput.isBlank()) {
            return "{}";
        }

        String cleaned = rawLlmOutput.trim();

        // 1. Remove ```json and ``` fences
        if (cleaned.contains("```json")) {
            int start = cleaned.indexOf("```json") + 7;
            int end = cleaned.indexOf("```", start);
            if (end != -1) {
                cleaned = cleaned.substring(start, end);
            } else {
                cleaned = cleaned.substring(start);
            }
        } else if (cleaned.contains("```")) {
            int start = cleaned.indexOf("```") + 3;
            int end = cleaned.indexOf("```", start);
            if (end != -1) {
                cleaned = cleaned.substring(start, end);
            } else {
                cleaned = cleaned.substring(start);
            }
        }

        // 2. Locate first '{' and last '}'
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        return cleaned.trim();
    }
}
