package code;

import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Transaction Manager & Optimistic Locking Simulator.
 *
 * Simulates:
 * 1. Spring's PlatformTransactionManager (BEGIN, COMMIT, ROLLBACK).
 * 2. Automatic Rollback on RuntimeException.
 * 3. OptimisticLockException detection on concurrent version mismatch.
 */
public class TransactionManagerSimulator {

    public static class OptimisticLockException extends RuntimeException {
        public OptimisticLockException(String message) {
            super(message);
        }
    }

    @FunctionalInterface
    public interface TransactionalTask<T> {
        T execute() throws Exception;
    }

    private final Map<Long, TokenQuotaEntity> database = new HashMap<>();

    public TransactionManagerSimulator() {
        TokenQuotaEntity initial = new TokenQuotaEntity(1L, "org_acme_corp", 100000);
        database.put(1L, initial);
    }

    public TokenQuotaEntity findById(Long id) {
        TokenQuotaEntity entity = database.get(id);
        return entity != null ? entity.copy() : null;
    }

    /**
     * Simulates Spring's @Transactional AOP proxy execution:
     * 1. Begin transaction
     * 2. Execute business code
     * 3. Commit on success
     * 4. Rollback on exception
     */
    public <T> T executeInTransaction(String txName, TransactionalTask<T> task) throws Exception {
        System.out.println("  [TX BEGIN] Starting transaction: " + txName);
        Map<Long, TokenQuotaEntity> txSnapshot = cloneDatabase();

        try {
            T result = task.execute();
            System.out.println("  [TX COMMIT] Successfully committed transaction: " + txName);
            return result;
        } catch (Exception ex) {
            System.out.println("  [TX ROLLBACK] Exception caught: " + ex.getMessage() + ". Rolling back transaction: " + txName);
            // Restore snapshot
            database.clear();
            for (Map.Entry<Long, TokenQuotaEntity> entry : txSnapshot.entrySet()) {
                database.put(entry.getKey(), entry.getValue().copy());
            }
            throw ex;
        }
    }

    /**
     * Simulates JPA em.merge() with Optimistic Lock check:
     * UPDATE token_quotas SET balance = ?, version = version + 1 WHERE id = ? AND version = ?
     */
    public synchronized void updateWithOptimisticLock(TokenQuotaEntity entity) {
        TokenQuotaEntity currentDb = database.get(entity.getId());
        if (currentDb == null) {
            throw new IllegalArgumentException("Entity with ID " + entity.getId() + " does not exist in database");
        }

        if (currentDb.getVersion() != entity.getVersion()) {
            throw new OptimisticLockException(
                "Row was updated or deleted by another transaction (expected version: " 
                + entity.getVersion() + ", actual database version: " + currentDb.getVersion() + ")"
            );
        }

        // Increment version and save
        TokenQuotaEntity updated = entity.copy();
        updated.setVersion(currentDb.getVersion() + 1);
        database.put(entity.getId(), updated);

        System.out.println("  [SQL UPDATE] UPDATE token_quotas SET balance = " + updated.getRemainingTokens() 
            + ", version = " + updated.getVersion() + " WHERE id = " + updated.getId() 
            + " AND version = " + entity.getVersion());
    }

    private Map<Long, TokenQuotaEntity> cloneDatabase() {
        Map<Long, TokenQuotaEntity> copy = new HashMap<>();
        for (Map.Entry<Long, TokenQuotaEntity> entry : database.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }
}
