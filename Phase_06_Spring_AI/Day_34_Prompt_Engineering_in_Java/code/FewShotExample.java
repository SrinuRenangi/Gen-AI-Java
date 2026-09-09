package com.genai.springai.prompt;

/**
 * Represents a single training demonstration example for Few-Shot prompting.
 */
public record FewShotExample(
        String input,
        String output
) {}
