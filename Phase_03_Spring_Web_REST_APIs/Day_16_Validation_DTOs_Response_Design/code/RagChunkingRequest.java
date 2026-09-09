package code;

/**
 * Record DTO demonstrating cross-field validation for RAG pipelines.
 *
 * Rule: chunkSize must be strictly greater than chunkOverlap.
 * If chunkOverlap >= chunkSize, the sliding window cannot advance,
 * causing infinite loops or OutOfMemory errors during text splitting.
 */
public record RagChunkingRequest(
    String documentId,
    int chunkSize,
    int chunkOverlap,
    String splitStrategy
) {
    public RagChunkingRequest {
        if (splitStrategy == null || splitStrategy.isBlank()) {
            splitStrategy = "RECURSIVE_CHARACTER";
        }
    }
}
