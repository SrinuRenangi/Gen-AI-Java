package code;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Enterprise pgvector Simulator.
 *
 * Recreates PostgreSQL pgvector behavior:
 * 1. Storing high-dimensional vector embeddings.
 * 2. Executing exact k-NN search via Cosine Distance (<=>).
 * 3. Hybrid Queries: Joining relational filters (tenant_id) with vector similarity.
 */
public class PgVectorSimulator {

    public record DocumentChunkRecord(
        Long id,
        String documentId,
        String tenantId,
        String content,
        float[] embedding
    ) {}

    public record SearchResult(
        DocumentChunkRecord chunk,
        double cosineDistance,
        double cosineSimilarity
    ) {}

    private final List<DocumentChunkRecord> table = new ArrayList<>();

    public void insert(Long id, String documentId, String tenantId, String content, float[] embedding) {
        table.add(new DocumentChunkRecord(id, documentId, tenantId, content, embedding));
    }

    /**
     * Executes simulated PostgreSQL query:
     * SELECT *, 1 - (embedding <=> :queryVector) as similarity
     * FROM document_chunks
     * WHERE tenant_id = :tenantId
     * ORDER BY embedding <=> :queryVector ASC
     * LIMIT :topK;
     */
    public List<SearchResult> searchNearestNeighbors(String tenantId, float[] queryVector, int topK) {
        System.out.println("  [PostgreSQL pgvector Query] Executing:");
        System.out.println("    SELECT id, document_id, content, 1 - (embedding <=> '" 
            + VectorMath.toPgVectorLiteral(queryVector) + "') AS similarity");
        System.out.println("    FROM document_chunks");
        System.out.println("    WHERE tenant_id = '" + tenantId + "'");
        System.out.println("    ORDER BY embedding <=> '" + VectorMath.toPgVectorLiteral(queryVector) + "' ASC");
        System.out.println("    LIMIT " + topK + ";");

        return table.stream()
            .filter(row -> tenantId == null || Objects.equals(row.tenantId(), tenantId))
            .map(row -> {
                double distance = VectorMath.cosineDistance(queryVector, row.embedding());
                double similarity = VectorMath.cosineSimilarity(queryVector, row.embedding());
                return new SearchResult(row, distance, similarity);
            })
            .sorted(Comparator.comparingDouble(SearchResult::cosineDistance))
            .limit(topK)
            .toList();
    }

    public int size() {
        return table.size();
    }
}
