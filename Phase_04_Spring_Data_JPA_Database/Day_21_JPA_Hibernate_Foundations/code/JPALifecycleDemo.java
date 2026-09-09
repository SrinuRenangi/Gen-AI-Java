package code;

/**
 * Driver class demonstrating Day 21: JPA & Hibernate Foundations.
 *
 * Demonstrates:
 * 1. The 4 Entity States: Transient, Managed, Detached, Removed.
 * 2. The First-Level Cache (Identity Map): Reference equality (==) for loaded entities.
 * 3. Automatic Dirty Checking: Hibernate detecting setter changes without explicit save() calls.
 * 4. Write-Behind (Flush): Defers SQL generation until transaction boundary.
 */
public class JPALifecycleDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 21: JPA & HIBERNATE FOUNDATIONS — PERSISTENCE CONTEXT & ENTITY LIFECYCLE   ");
        System.out.println("================================================================================");

        MiniPersistenceContext em = new MiniPersistenceContext();

        // -------------------------------------------------------------------------
        // SCENARIO 1: TRANSIENT -> MANAGED -> SQL INSERT
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: Transient -> Managed -> Flush (INSERT) ---");
        PromptEntity newPrompt = new PromptEntity(
            "code-explainer-prompt",
            "Explain the following Java code step-by-step: {code}",
            "claude-3-5-sonnet"
        );
        System.out.println(" 1. Instantiated new entity with 'new': State = " + em.getState(newPrompt));

        em.persist(newPrompt);
        System.out.println(" 2. Called em.persist(): State = " + em.getState(newPrompt) + ", Assigned ID = " + newPrompt.getId());

        // At this point, NO SQL INSERT has executed yet! (Write-behind pattern)
        System.out.println(" 3. Before flush, SQL statement count: " + em.getSqlExecutionLog().size());
        em.flush();
        System.out.println(" 4. After flush, SQL statement count: " + em.getSqlExecutionLog().size());

        // -------------------------------------------------------------------------
        // SCENARIO 2: FIRST-LEVEL CACHE (L1 CACHE) & IDENTITY MAP
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: First-Level Cache (Identity Map Proof) ---");
        System.out.println(" Fetching prompt ID 1 for the first time:");
        PromptEntity firstFetch = em.find(1L);

        System.out.println("\n Fetching prompt ID 1 for the second time in same persistence context:");
        PromptEntity secondFetch = em.find(1L);

        boolean sameReference = (firstFetch == secondFetch);
        System.out.println(" Are both object references IDENTICAL in JVM memory (firstFetch == secondFetch)? " + sameReference);
        if (!sameReference) {
            throw new AssertionError("L1 Cache violation: Entity references must be identical!");
        }

        // -------------------------------------------------------------------------
        // SCENARIO 3: AUTOMATIC DIRTY CHECKING (No em.save() Needed!)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: Automatic Dirty Checking on Managed Entity ---");
        System.out.println(" Modifying template content via standard Java setter...");
        firstFetch.setTemplateContent("UPDATED: You are a senior Java architect. Explain: {code}");
        System.out.println(" Note: We NEVER called em.update() or em.save()!");

        em.flush(); // Flush triggers snapshot comparison and emits SQL UPDATE

        // -------------------------------------------------------------------------
        // SCENARIO 4: DETACHED STATE (Changes Are Ignored)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 4: Detached Entity (Modifications Ignored) ---");
        em.detach(firstFetch);
        System.out.println(" Called em.detach(): State = " + em.getState(firstFetch));

        firstFetch.setName("THIS_CHANGE_SHOULD_NEVER_REACH_THE_DATABASE");
        System.out.println(" Modified detached entity name. Triggering flush...");
        em.flush(); // Nothing emitted for detached entity!

        // -------------------------------------------------------------------------
        // SCENARIO 5: REMOVED STATE -> SQL DELETE
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 5: Managed -> Removed -> Flush (DELETE) ---");
        System.out.println(" Removing prompt ID " + newPrompt.getId() + "...");
        em.remove(newPrompt);
        System.out.println(" State after em.remove(): " + em.getState(newPrompt));

        em.flush(); // Emits SQL DELETE

        System.out.println("\n================================================================================");
        System.out.println(" COMPLETE AUDIT OF GENERATED SQL STATEMENTS:");
        for (int i = 0; i < em.getSqlExecutionLog().size(); i++) {
            System.out.printf("   [%d] %s%n", i + 1, em.getSqlExecutionLog().get(i));
        }
        System.out.println("================================================================================");
        System.out.println(" DAY 21 DEMONSTRATION COMPLETE: ALL JPA LIFECYCLE PATTERNS VERIFIED!          ");
        System.out.println("================================================================================");
    }
}
