package code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pure Java simulation of Hibernate's First-Level Cache and Persistence Context.
 *
 * Demonstrates how Hibernate works internally:
 * 1. Identity Map: Guarantees `em.find(Prompt.class, 1L) == em.find(Prompt.class, 1L)` (same reference in memory).
 * 2. Snapshot Map: Takes a copy of the entity when loaded/persisted.
 * 3. Dirty Checking: On flush(), compares entity against snapshot and issues SQL UPDATE only if fields changed.
 * 4. Write-Behind Cache: Defers SQL statements until flush() / commit().
 */
public class MiniPersistenceContext {

    private final AtomicLong idSequence = new AtomicLong(100);

    // 1. First-Level Cache (Identity Map): ID -> Entity
    private final Map<Long, PromptEntity> firstLevelCache = new HashMap<>();

    // 2. Snapshot Map: ID -> Snapshot Copy
    private final Map<Long, PromptEntity> snapshots = new HashMap<>();

    // 3. State Tracking: Entity -> EntityState
    private final Map<PromptEntity, EntityState> entityStates = new HashMap<>();

    // Simulated database storage
    private final Map<Long, PromptEntity> databaseTable = new HashMap<>();

    // Simulated SQL statement audit log
    private final List<String> sqlExecutionLog = new ArrayList<>();

    public MiniPersistenceContext() {
        // Pre-populate database with one existing prompt
        PromptEntity existing = new PromptEntity(
            "default-rag-prompt",
            "Answer the query using the following context: {context}. Query: {query}",
            "gpt-4o"
        );
        existing.setId(1L);
        databaseTable.put(1L, existing.createSnapshot());
    }

    /**
     * JPA em.persist(entity):
     * Transitions TRANSIENT -> MANAGED.
     * Assigns generated database ID, puts in first-level cache, takes snapshot.
     */
    public void persist(PromptEntity entity) {
        if (entity.getId() == null) {
            long generatedId = idSequence.incrementAndGet();
            entity.setId(generatedId);
        }
        firstLevelCache.put(entity.getId(), entity);
        snapshots.put(entity.getId(), entity.createSnapshot());
        entityStates.put(entity, EntityState.MANAGED);

        System.out.println("  [JPA persist] Entity transitioned to MANAGED: " + entity);
    }

    /**
     * JPA em.find(Class, id):
     * Checks First-Level Cache first.
     * If cache hit -> returns existing Java reference (Zero SQL queries!).
     * If cache miss -> queries database, populates cache, takes snapshot.
     */
    public PromptEntity find(Long id) {
        if (firstLevelCache.containsKey(id)) {
            System.out.println("  [JPA find] L1 CACHE HIT! Returning existing managed instance for ID: " + id);
            return firstLevelCache.get(id);
        }

        System.out.println("  [JPA find] L1 CACHE MISS. Querying database: SELECT * FROM prompt_templates WHERE id = " + id);
        sqlExecutionLog.add("SELECT * FROM prompt_templates WHERE id = " + id);

        PromptEntity dbRow = databaseTable.get(id);
        if (dbRow == null) {
            return null;
        }

        PromptEntity managed = dbRow.createSnapshot();
        firstLevelCache.put(id, managed);
        snapshots.put(id, managed.createSnapshot());
        entityStates.put(managed, EntityState.MANAGED);

        return managed;
    }

    /**
     * JPA em.remove(entity):
     * Transitions MANAGED -> REMOVED.
     */
    public void remove(PromptEntity entity) {
        if (entityStates.get(entity) != EntityState.MANAGED) {
            throw new IllegalStateException("Entity must be in MANAGED state to be removed: " + entity);
        }
        entityStates.put(entity, EntityState.REMOVED);
        System.out.println("  [JPA remove] Entity marked as REMOVED: ID " + entity.getId());
    }

    /**
     * JPA em.detach(entity):
     * Transitions MANAGED -> DETACHED.
     * Removes entity from L1 cache and snapshots. Changes to this instance will NO LONGER be tracked.
     */
    public void detach(PromptEntity entity) {
        if (entity.getId() != null) {
            firstLevelCache.remove(entity.getId());
            snapshots.remove(entity.getId());
        }
        entityStates.put(entity, EntityState.DETACHED);
        System.out.println("  [JPA detach] Entity transitioned to DETACHED: ID " + entity.getId());
    }

    /**
     * JPA em.flush():
     * Synchronizes in-memory persistence context with the database.
     * Executes Dirty Checking and emits INSERT, UPDATE, DELETE SQL statements.
     */
    public void flush() {
        System.out.println("\n  --- EXECUTING JPA FLUSH (DIRTY CHECKING & SQL SYNCHRONIZATION) ---");

        for (Map.Entry<PromptEntity, EntityState> entry : new HashMap<>(entityStates).entrySet()) {
            PromptEntity entity = entry.getKey();
            EntityState state = entry.getValue();

            switch (state) {
                case MANAGED -> {
                    PromptEntity snap = snapshots.get(entity.getId());
                    if (!databaseTable.containsKey(entity.getId())) {
                        // New insert
                        String sql = "INSERT INTO prompt_templates (id, name, template_content, model_family, version, active) VALUES ("
                            + entity.getId() + ", '" + entity.getName() + "', '" + entity.getTemplateContent() + "', '" 
                            + entity.getModelFamily() + "', " + entity.getVersion() + ", " + entity.isActive() + ")";
                        sqlExecutionLog.add(sql);
                        databaseTable.put(entity.getId(), entity.createSnapshot());
                        snapshots.put(entity.getId(), entity.createSnapshot());
                        System.out.println("  [SQL INSERT] " + sql);
                    } else if (entity.isDirty(snap)) {
                        // Dirty checking detected modifications!
                        String sql = "UPDATE prompt_templates SET name = '" + entity.getName() + "', template_content = '"
                            + entity.getTemplateContent() + "', model_family = '" + entity.getModelFamily() 
                            + "', version = " + (entity.getVersion() + 1) + ", active = " + entity.isActive() 
                            + " WHERE id = " + entity.getId();
                        entity.setVersion(entity.getVersion() + 1);
                        sqlExecutionLog.add(sql);
                        databaseTable.put(entity.getId(), entity.createSnapshot());
                        snapshots.put(entity.getId(), entity.createSnapshot());
                        System.out.println("  [SQL UPDATE (Dirty Checking)] " + sql);
                    } else {
                        System.out.println("  [DIRTY CHECKING] Entity ID " + entity.getId() + " is UNCHANGED. Zero SQL generated.");
                    }
                }
                case REMOVED -> {
                    String sql = "DELETE FROM prompt_templates WHERE id = " + entity.getId();
                    sqlExecutionLog.add(sql);
                    databaseTable.remove(entity.getId());
                    firstLevelCache.remove(entity.getId());
                    snapshots.remove(entity.getId());
                    entityStates.remove(entity);
                    System.out.println("  [SQL DELETE] " + sql);
                }
                case DETACHED, TRANSIENT -> {
                    // Untracked, ignore
                }
            }
        }
    }

    public EntityState getState(PromptEntity entity) {
        return entityStates.getOrDefault(entity, EntityState.TRANSIENT);
    }

    public List<String> getSqlExecutionLog() {
        return List.copyOf(sqlExecutionLog);
    }
}
