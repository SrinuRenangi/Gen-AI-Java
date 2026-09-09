package com.javagenai.day11;

public record VectorStoreProperties(
    String host,
    int port,
    String indexName,
    int vectorDimensions
) {
    public VectorStoreProperties {
        if (vectorDimensions <= 0) {
            throw new IllegalArgumentException("Vector dimensions must be > 0. Received: " + vectorDimensions);
        }
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
    }
}
