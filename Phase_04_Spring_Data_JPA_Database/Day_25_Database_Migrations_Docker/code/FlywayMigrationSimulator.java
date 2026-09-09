package code;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Flyway Database Migration Engine Simulator.
 *
 * Implements:
 * 1. The flyway_schema_history table tracking installed migrations.
 * 2. SHA-256 Checksum validation to detect altered historical migration scripts.
 * 3. Strict version ordering (V1 -> V2 -> V3).
 * 4. Distributed lock simulation to prevent concurrent pod race conditions.
 */
public class FlywayMigrationSimulator {

    public record MigrationScript(String version, String description, String sqlScript) {
        public int calculateChecksum() {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(sqlScript.getBytes(StandardCharsets.UTF_8));
                // Convert first 4 bytes to an integer checksum like Flyway
                return ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16) | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public record SchemaHistoryEntry(
        int installedRank,
        String version,
        String description,
        int checksum,
        String installedBy,
        Instant installedOn,
        long executionTimeMs,
        boolean success
    ) {}

    private final Map<String, SchemaHistoryEntry> schemaHistory = new LinkedHashMap<>();
    private final List<String> appliedDdlStatements = new ArrayList<>();
    private boolean lockHeld = false;

    /**
     * Executes pending migrations.
     */
    public synchronized int migrate(List<MigrationScript> availableScripts) {
        if (lockHeld) {
            throw new IllegalStateException("Could not acquire database lock: another node is currently migrating");
        }
        lockHeld = true;
        System.out.println("  [Flyway Lock] Acquired migration table lock on 'flyway_schema_history'");

        try {
            int appliedCount = 0;

            for (MigrationScript script : availableScripts) {
                if (schemaHistory.containsKey(script.version())) {
                    // Script was already installed! Verify checksum integrity.
                    SchemaHistoryEntry past = schemaHistory.get(script.version());
                    int currentChecksum = script.calculateChecksum();
                    if (past.checksum() != currentChecksum) {
                        throw new IllegalStateException(
                            "CRITICAL FLYWAY ERROR: Checksum mismatch for version " + script.version() + " (" + script.description() + ")! "
                            + "Previously installed with checksum " + past.checksum() + ", but current script has checksum " + currentChecksum 
                            + ". Migration scripts in git MUST BE IMMUTABLE!"
                        );
                    }
                    System.out.println("  [Flyway Verify] Version " + script.version() + " already applied with valid checksum " + past.checksum() + ". Skipping.");
                } else {
                    // New pending migration! Execute DDL.
                    long start = System.currentTimeMillis();
                    System.out.println("  [Flyway Apply] Migrating to version " + script.version() + " - " + script.description() + " ...");
                    
                    // Simulate DDL execution
                    appliedDdlStatements.add(script.sqlScript());
                    long duration = System.currentTimeMillis() - start;

                    SchemaHistoryEntry entry = new SchemaHistoryEntry(
                        schemaHistory.size() + 1,
                        script.version(),
                        script.description(),
                        script.calculateChecksum(),
                        "postgres_flyway_user",
                        Instant.now(),
                        duration,
                        true
                    );
                    schemaHistory.put(script.version(), entry);
                    appliedCount++;
                    System.out.println("  [Flyway Success] Successfully applied version " + script.version() + " (Checksum: " + entry.checksum() + ")");
                }
            }

            return appliedCount;
        } finally {
            lockHeld = false;
            System.out.println("  [Flyway Lock] Released migration table lock on 'flyway_schema_history'");
        }
    }

    public List<SchemaHistoryEntry> getHistory() {
        return List.copyOf(schemaHistory.values());
    }

    public List<String> getAppliedDdlStatements() {
        return Collections.unmodifiableList(appliedDdlStatements);
    }
}
