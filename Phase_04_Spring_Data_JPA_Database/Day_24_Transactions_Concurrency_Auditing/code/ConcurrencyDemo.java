package code;

/**
 * Driver class demonstrating Day 24: Transactions, Concurrency & Auditing.
 *
 * Demonstrates:
 * 1. ACID Transaction Atomicity: Successful commit vs Rollback on error.
 * 2. Optimistic Locking (@Version): Concurrent update conflict detection.
 * 3. Automated auditing: LastModifiedBy and timestamp updates.
 */
public class ConcurrencyDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 24: TRANSACTIONS, CONCURRENCY & AUDITING (@Transactional, @Version)       ");
        System.out.println("================================================================================");

        TransactionManagerSimulator tm = new TransactionManagerSimulator();

        // -------------------------------------------------------------------------
        // SCENARIO 1: SUCCESSFUL TRANSACTION COMMIT
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: Successful @Transactional Execution ---");
        try {
            tm.executeInTransaction("deduct-tokens-prompt-1", () -> {
                TokenQuotaEntity quota = tm.findById(1L);
                System.out.println(" Loaded quota: " + quota);
                quota.deductTokens(15000, "user_alice");
                tm.updateWithOptimisticLock(quota);
                return quota;
            });
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }

        TokenQuotaEntity afterCommit = tm.findById(1L);
        System.out.println(" Verified balance after commit: " + afterCommit);

        // -------------------------------------------------------------------------
        // SCENARIO 2: TRANSACTION ROLLBACK ON EXCEPTION (ATOMICITY VERIFIED)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: Transaction Rollback on RuntimeException ---");
        try {
            tm.executeInTransaction("failing-ai-pipeline", () -> {
                TokenQuotaEntity quota = tm.findById(1L);
                quota.deductTokens(20000, "user_bob");
                tm.updateWithOptimisticLock(quota);
                System.out.println(" In-flight deducted balance: " + quota.getRemainingTokens());

                // Simulate downstream LLM failure triggering rollback
                throw new RuntimeException("Upstream OpenAI returned HTTP 500. Aborting transaction!");
            });
        } catch (Exception e) {
            System.out.println(" Handled caught exception: " + e.getMessage());
        }

        TokenQuotaEntity afterRollback = tm.findById(1L);
        System.out.println(" Verified balance after rollback (should remain 85000): " + afterRollback.getRemainingTokens());
        if (afterRollback.getRemainingTokens() != 85000) {
            throw new AssertionError("Atomicity failure: Rollback did not restore initial balance!");
        }

        // -------------------------------------------------------------------------
        // SCENARIO 3: OPTIMISTIC LOCKING COLLISION (@Version)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: Optimistic Locking Collision Detection ---");
        System.out.println(" Simulating two concurrent Virtual Threads updating the same quota:");

        // Virtual Thread 1 reads state (version = 2)
        TokenQuotaEntity thread1Copy = tm.findById(1L);
        System.out.println(" [Thread 1] Read quota: version = " + thread1Copy.getVersion() + ", balance = " + thread1Copy.getRemainingTokens());

        // Virtual Thread 2 reads state simultaneously (version = 2)
        TokenQuotaEntity thread2Copy = tm.findById(1L);
        System.out.println(" [Thread 2] Read quota: version = " + thread2Copy.getVersion() + ", balance = " + thread2Copy.getRemainingTokens());

        // Thread 1 completes first and commits:
        System.out.println("\n [Thread 1] Deducting 5000 tokens and committing...");
        thread1Copy.deductTokens(5000, "virtual_thread_1");
        tm.updateWithOptimisticLock(thread1Copy); // Succeeds, version becomes 3!

        // Thread 2 tries to commit with stale version 2:
        System.out.println("\n [Thread 2] Deducting 10000 tokens and attempting to commit with stale version " + thread2Copy.getVersion() + "...");
        try {
            thread2Copy.deductTokens(10000, "virtual_thread_2");
            tm.updateWithOptimisticLock(thread2Copy); // Will collide and throw!
            System.out.println(" ERROR: Should not have succeeded!");
        } catch (TransactionManagerSimulator.OptimisticLockException ole) {
            System.out.println(" [COLLISION DETECTED] OptimisticLockException caught successfully!");
            System.out.println(" Detail: " + ole.getMessage());
            System.out.println(" Resolution: Application will reload fresh entity and retry safely.");
        }

        TokenQuotaEntity finalQuota = tm.findById(1L);
        System.out.println("\n Final Quota State in Database: " + finalQuota);

        System.out.println("\n================================================================================");
        System.out.println(" DAY 24 DEMONSTRATION COMPLETE: TRANSACTIONS & LOCKING FULLY VERIFIED!          ");
        System.out.println("================================================================================");
    }
}
