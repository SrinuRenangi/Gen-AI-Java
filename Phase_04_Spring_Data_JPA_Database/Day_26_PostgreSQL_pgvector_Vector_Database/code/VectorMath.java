package code;

/**
 * Mathematical foundation for high-dimensional vector similarity.
 *
 * Implements the core distance metrics used by PostgreSQL pgvector:
 * 1. Cosine Distance (<=>): 1 - (A . B) / (||A|| * ||B||)
 * 2. Euclidean L2 Distance (<->): sqrt(sum((A_i - B_i)^2))
 * 3. Dot Product / Inner Product (<#>): - (A . B)
 */
public final class VectorMath {

    private VectorMath() {}

    /**
     * Calculates Cosine Similarity between two high-dimensional vectors.
     * Range: -1.0 to 1.0 (1.0 = identical angle, 0.0 = orthogonal, -1.0 = diametrically opposite).
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        validateDimensions(a, b);

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Calculates PostgreSQL pgvector Cosine Distance (<=> operator).
     * Cosine Distance = 1.0 - Cosine Similarity.
     * Range: 0.0 (identical) to 2.0 (opposite).
     */
    public static double cosineDistance(float[] a, float[] b) {
        return 1.0 - cosineSimilarity(a, b);
    }

    /**
     * Calculates PostgreSQL pgvector Euclidean L2 Distance (<-> operator).
     */
    public static double euclideanDistance(float[] a, float[] b) {
        validateDimensions(a, b);

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Formats a float array into a PostgreSQL pgvector string literal: '[0.12, -0.45, 0.98]'
     */
    public static String toPgVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(String.format("%.4f", vector[i]));
            if (i < vector.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private static void validateDimensions(float[] a, float[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Vectors must not be null");
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                "Dimension mismatch: Vector A has dimension " + a.length + " but Vector B has dimension " + b.length
            );
        }
    }
}
