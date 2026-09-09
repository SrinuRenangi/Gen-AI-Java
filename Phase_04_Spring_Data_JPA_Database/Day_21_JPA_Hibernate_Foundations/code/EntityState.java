package code;

/**
 * The 4 canonical JPA Entity Lifecycle States.
 *
 * 1. TRANSIENT: Newly instantiated with `new PromptEntity(...)`. No database identity (ID is null), not managed by any Persistence Context.
 * 2. MANAGED: Associated with an active Persistence Context. Changes are tracked automatically via Dirty Checking.
 * 3. DETACHED: Was managed, but the Persistence Context / Transaction closed. Holds a database ID, but changes are not tracked.
 * 4. REMOVED: Marked for deletion via `em.remove()`. Will be deleted from the database upon flush/commit.
 */
public enum EntityState {
    TRANSIENT,
    MANAGED,
    DETACHED,
    REMOVED
}
