package com.javagenai.day10;

public class VectorDatabaseConnector {

    public void cleanup() {
        System.out.println("[VectorDatabaseConnector - @PreDestroy]");
        System.out.println("   -> Flushing in-memory vector index caches...");
        System.out.println("   -> ✅ Disconnected PostgreSQL pgvector connection pool cleanly.");
    }
}
