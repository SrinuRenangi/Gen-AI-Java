package com.genai.springai.embeddings;

/**
 * High-performance vector arithmetic utilities in pure Java 21:
 * - Dot Product
 * - Euclidean Vector Magnitude (L2 Norm)
 * - Cosine Similarity
 * - In-place Vector Normalization
 */
public final class VectorMath {

    private VectorMath() {}

    public static double dotProduct(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector dimension mismatch: " + a.length + " != " + b.length);
        }
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    public static double magnitude(float[] a) {
        double sumSquares = 0.0;
        for (float v : a) {
            sumSquares += v * v;
        }
        return Math.sqrt(sumSquares);
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        double magA = magnitude(a);
        double magB = magnitude(b);
        if (magA == 0.0 || magB == 0.0) {
            return 0.0;
        }
        return dotProduct(a, b) / (magA * magB);
    }

    public static float[] normalize(float[] a) {
        double mag = magnitude(a);
        if (mag == 0.0) return a;
        float[] normalized = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            normalized[i] = (float) (a[i] / mag);
        }
        return normalized;
    }
}
