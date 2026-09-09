package code;

import java.util.ArrayList;
import java.util.List;

/**
 * Driver class demonstrating Day 25: Database Migrations (Flyway) & Docker.
 *
 * Demonstrates:
 * 1. Applying versioned SQL migrations (V1, V2) containing pgvector extension scripts.
 * 2. Idempotent application startup (no-op on second boot).
 * 3. Checksum verification and tamper detection preventing corrupt database states.
 */
public class MigrationDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 25: DATABASE MIGRATIONS (FLYWAY) & DOCKER INFRASTRUCTURE SETUP             ");
        System.out.println("================================================================================");

        FlywayMigrationSimulator flyway = new FlywayMigrationSimulator();

        // Define initial migration scripts
        FlywayMigrationSimulator.MigrationScript v1 = new FlywayMigrationSimulator.MigrationScript(
            "1",
            "init_conversation_sessions",
            """
            CREATE TABLE conversation_sessions (
                id BIGSERIAL PRIMARY KEY,
                user_id VARCHAR(64) NOT NULL,
                title VARCHAR(200) NOT NULL,
                total_tokens INTEGER NOT NULL DEFAULT 0,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            CREATE INDEX idx_sessions_user_id ON conversation_sessions(user_id);
            """
        );

        FlywayMigrationSimulator.MigrationScript v2 = new FlywayMigrationSimulator.MigrationScript(
            "2",
            "enable_pgvector_and_chunks",
            """
            CREATE EXTENSION IF NOT EXISTS vector;
            CREATE TABLE document_chunks (
                id BIGSERIAL PRIMARY KEY,
                document_id VARCHAR(120) NOT NULL,
                chunk_index INTEGER NOT NULL,
                content TEXT NOT NULL,
                embedding vector(1536) NOT NULL,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            CREATE INDEX idx_chunks_hnsw ON document_chunks USING hnsw (embedding vector_cosine_ops);
            """
        );

        List<FlywayMigrationSimulator.MigrationScript> scripts = List.of(v1, v2);

        // -------------------------------------------------------------------------
        // SCENARIO 1: FIRST APPLICATION STARTUP (MIGRATIONS APPLIED)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: First Application Boot (Applying V1 & V2) ---");
        int applied = flyway.migrate(scripts);
        System.out.println(" Migrations applied: " + applied);

        System.out.println("\n flyway_schema_history Table State:");
        for (FlywayMigrationSimulator.SchemaHistoryEntry entry : flyway.getHistory()) {
            System.out.printf("   [Rank %d] Version: %s | Description: %-28s | Checksum: %-12d | Success: %b%n",
                entry.installedRank(), entry.version(), entry.description(), entry.checksum(), entry.success());
        }

        // -------------------------------------------------------------------------
        // SCENARIO 2: SECOND APPLICATION BOOT (IDEMPOTENT NO-OP)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: Second Application Boot (Idempotent Check) ---");
        int secondRunApplied = flyway.migrate(scripts);
        System.out.println(" Migrations applied on reboot: " + secondRunApplied + " (Expected: 0)");

        // -------------------------------------------------------------------------
        // SCENARIO 3: CHECKSUM TAMPER DETECTION
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: Detecting Altered Historical Migration Script ---");
        // A developer maliciously or accidentally edits V1 after it was already deployed to production
        FlywayMigrationSimulator.MigrationScript tamperedV1 = new FlywayMigrationSimulator.MigrationScript(
            "1",
            "init_conversation_sessions",
            """
            CREATE TABLE conversation_sessions (
                id BIGSERIAL PRIMARY KEY,
                user_id VARCHAR(64) NOT NULL,
                title VARCHAR(200) NOT NULL,
                total_tokens INTEGER NOT NULL DEFAULT 0,
                tampered_column VARCHAR(100) NOT NULL, -- ⚠️ ILLEGAL EDIT IN HISTORICAL SCRIPT!
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """
        );

        List<FlywayMigrationSimulator.MigrationScript> tamperedScripts = List.of(tamperedV1, v2);
        try {
            System.out.println(" Attempting to boot application with tampered V1 migration script...");
            flyway.migrate(tamperedScripts);
            System.out.println(" ERROR: Should not have allowed tampered migration!");
        } catch (IllegalStateException ex) {
            System.out.println(" [SAFETY GUARD ACTIVATED] Checksum mismatch caught cleanly!");
            System.out.println(" Message: " + ex.getMessage());
        }

        System.out.println("\n================================================================================");
        System.out.println(" DAY 25 DEMONSTRATION COMPLETE: FLYWAY & DOCKER ENGINE VERIFIED!                ");
        System.out.println("================================================================================");
    }
}
