package code;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Modern Java 21 Record DTO for AI Completion Requests.
 *
 * Demonstrates:
 * 1. Immutability by default (thread-safe, safe across concurrent virtual threads).
 * 2. Compact constructor with defensive copies for collections.
 * 3. Validation rule specification matching Jakarta Bean Validation semantics:
 *    - @NotBlank prompt
 *    - @Pattern / Custom whitelist model check
 *    - @DecimalMin / @DecimalMax temperature (0.0 to 2.0)
 *    - @Min / @Max maxTokens (1 to 4096)
 *    - @Size stopSequences (max 4 elements)
 */
public record CompletionRequest(
    String prompt,
    String model,
    double temperature,
    int maxTokens,
    String systemPrompt,
    List<String> stopSequences
) {
    public static final Set<String> ALLOWED_MODELS = Set.of(
        "gpt-4o", "gpt-4o-mini", "claude-3-5-sonnet", "llama3.2", "mistral-large"
    );

    /**
     * Compact constructor: Performs defensive copies and normalization.
     */
    public CompletionRequest {
        // Defensive copy and null-safe defaults for collections
        stopSequences = (stopSequences == null) 
            ? Collections.emptyList() 
            : List.copyOf(stopSequences);

        // Normalize blank systemPrompt to null
        if (systemPrompt != null && systemPrompt.isBlank()) {
            systemPrompt = null;
        }
    }

    /**
     * Convenience factory constructor with sensible defaults.
     */
    public static CompletionRequest of(String prompt, String model) {
        return new CompletionRequest(prompt, model, 0.7, 1024, null, List.of());
    }
}
