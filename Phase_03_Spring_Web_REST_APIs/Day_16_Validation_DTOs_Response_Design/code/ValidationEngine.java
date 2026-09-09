package code;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validation Engine simulating Spring Boot's @Valid and Hibernate Validator.
 *
 * Demonstrates:
 * 1. Declarative constraint checking on Record DTOs.
 * 2. Cross-field validation (e.g. chunkSize vs chunkOverlap).
 * 3. Aggregating all constraint violations into an RFC 7807 ProblemDetail object.
 */
public class ValidationEngine {

    public record Violation(String field, String message, Object invalidValue) {}

    /**
     * Validates a CompletionRequest against enterprise AI gateway policies.
     */
    public List<Violation> validateCompletionRequest(CompletionRequest req) {
        List<Violation> violations = new ArrayList<>();

        // 1. prompt: @NotBlank, @Size(max = 4000)
        if (req.prompt() == null || req.prompt().isBlank()) {
            violations.add(new Violation("prompt", "Prompt must not be null, empty, or blank", req.prompt()));
        } else if (req.prompt().length() > 4000) {
            violations.add(new Violation(
                "prompt",
                "Prompt length (" + req.prompt().length() + ") exceeds maximum allowed limit of 4000 characters",
                req.prompt().length()
            ));
        }

        // 2. model: Whitelist check (@ApprovedModel)
        if (req.model() == null || req.model().isBlank()) {
            violations.add(new Violation("model", "Model name is required", req.model()));
        } else if (!CompletionRequest.ALLOWED_MODELS.contains(req.model())) {
            violations.add(new Violation(
                "model",
                "Model '" + req.model() + "' is not approved. Allowed models: " + CompletionRequest.ALLOWED_MODELS,
                req.model()
            ));
        }

        // 3. temperature: @DecimalMin("0.0"), @DecimalMax("2.0")
        if (req.temperature() < 0.0 || req.temperature() > 2.0) {
            violations.add(new Violation(
                "temperature",
                "Temperature must be between 0.0 (deterministic) and 2.0 (creative)",
                req.temperature()
            ));
        }

        // 4. maxTokens: @Min(1), @Max(4096)
        if (req.maxTokens() < 1 || req.maxTokens() > 4096) {
            violations.add(new Violation(
                "maxTokens",
                "maxTokens must be between 1 and 4096 tokens",
                req.maxTokens()
            ));
        }

        // 5. stopSequences: @Size(max = 4)
        if (req.stopSequences() != null && req.stopSequences().size() > 4) {
            violations.add(new Violation(
                "stopSequences",
                "A maximum of 4 stop sequences may be specified",
                req.stopSequences().size()
            ));
        }

        return violations;
    }

    /**
     * Validates a RagChunkingRequest including cross-field constraints.
     */
    public List<Violation> validateRagChunking(RagChunkingRequest req) {
        List<Violation> violations = new ArrayList<>();

        if (req.documentId() == null || req.documentId().isBlank()) {
            violations.add(new Violation("documentId", "Document ID must not be blank", req.documentId()));
        }

        if (req.chunkSize() < 50 || req.chunkSize() > 8000) {
            violations.add(new Violation("chunkSize", "chunkSize must be between 50 and 8000 tokens", req.chunkSize()));
        }

        if (req.chunkOverlap() < 0) {
            violations.add(new Violation("chunkOverlap", "chunkOverlap cannot be negative", req.chunkOverlap()));
        }

        // Cross-field validation: chunkOverlap must be strictly less than chunkSize
        if (req.chunkOverlap() >= req.chunkSize()) {
            violations.add(new Violation(
                "chunkOverlap",
                "chunkOverlap (" + req.chunkOverlap() + ") must be strictly less than chunkSize (" + req.chunkSize() + ") to allow window advancement",
                req.chunkOverlap()
            ));
        }

        return violations;
    }

    /**
     * Converts a list of violations into an RFC 7807 ProblemDetail object.
     */
    public Optional<ProblemDetail> toProblemDetail(List<Violation> violations, String instancePath) {
        if (violations.isEmpty()) {
            return Optional.empty();
        }

        ProblemDetail pd = ProblemDetail.forValidationFailure(
            "Validation failed with " + violations.size() + " constraint violation(s)",
            instancePath
        );

        for (Violation v : violations) {
            pd.addInvalidParam(v.field(), v.message(), v.invalidValue());
        }

        return Optional.of(pd);
    }
}
