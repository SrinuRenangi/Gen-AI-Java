package com.javagenai.day02;

public class Vector3D {
    private final double x;
    private final double y;
    private final double z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double dotProduct(Vector3D other) {
        if (other == null) throw new IllegalArgumentException("Target vector cannot be null");
        return (this.x * other.x) + (this.y * other.y) + (this.z * other.z);
    }

    public double magnitude() {
        return Math.sqrt((x * x) + (y * y) + (z * z));
    }

    public double cosineSimilarity(Vector3D other) {
        double dot = this.dotProduct(other);
        double denom = this.magnitude() * other.magnitude();
        if (denom == 0.0) return 0.0;
        return dot / denom;
    }

    @Override
    public String toString() {
        return String.format("Vector3D(%.4f, %.4f, %.4f)", x, y, z);
    }
}
