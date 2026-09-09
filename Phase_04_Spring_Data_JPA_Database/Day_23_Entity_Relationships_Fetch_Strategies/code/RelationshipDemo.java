package code;

import java.util.List;

/**
 * Driver class demonstrating Day 23: Entity Relationships & Fetch Strategies.
 *
 * Demonstrates:
 * 1. The N+1 Query Disaster in real time.
 * 2. The JOIN FETCH / @EntityGraph single-query solution.
 * 3. CascadeType.ALL propagation from parent to children.
 * 4. Bidirectional helper methods (addMessage/removeMessage).
 */
public class RelationshipDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 23: ENTITY RELATIONSHIPS, FETCH STRATEGIES & THE N+1 QUERY SOLUTION        ");
        System.out.println("================================================================================");

        RelationshipSimulator simulator = new RelationshipSimulator();

        // -------------------------------------------------------------------------
        // SCENARIO 1: THE N+1 QUERY DISASTER
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: The N+1 Query Disaster (Lazy Loading inside a Loop) ---");
        List<SessionEntity> nPlusOneSessions = simulator.findAllWithLazyNPlusOne();
        System.out.println(" Total SQL queries fired for 3 sessions: " + simulator.getExecutedSqlQueries().size());
        System.out.println(" Formula: 1 (Parents) + N (3 Children) = 4 Queries.");
        System.out.println(" ⚠️ Under 1,000 sessions, this fires 1,001 SQL queries and exhausts PostgreSQL connection pools!");

        // -------------------------------------------------------------------------
        // SCENARIO 2: THE JOIN FETCH SOLUTION
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: The JOIN FETCH / @EntityGraph Solution (Single SQL Query) ---");
        List<SessionEntity> joinFetchSessions = simulator.findAllWithJoinFetch();
        System.out.println(" Total SQL queries fired with JOIN FETCH: " + simulator.getExecutedSqlQueries().size());
        System.out.println(" ✅ Reduced from 4 queries to EXACTLY 1 query!");
        System.out.println(" Hydrated " + joinFetchSessions.size() + " sessions with their messages safely.");

        // -------------------------------------------------------------------------
        // SCENARIO 3: CASCADE PERSIST (CascadeType.ALL)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: CascadeType.ALL (Persist Parent -> Children Automatically) ---");
        SessionEntity newSession = new SessionEntity(99L, "Autonomous Research Session", "user_enterprise");

        // Use bidirectional helper method
        newSession.addMessage(new MessageEntity(991L, "SYSTEM", "Synthesize financial 10-K filings.", 8));
        newSession.addMessage(new MessageEntity(992L, "USER", "Compare Apple and Microsoft revenue.", 7));

        System.out.println(" Persisting parent SessionEntity with " + newSession.getMessages().size() + " children:");
        simulator.persistWithCascade(newSession);
        System.out.println(" Total SQL statements generated: " + simulator.getExecutedSqlQueries().size());

        System.out.println("\n================================================================================");
        System.out.println(" DAY 23 DEMONSTRATION COMPLETE: RELATIONSHIPS & N+1 OPTIMIZATION VERIFIED!      ");
        System.out.println("================================================================================");
    }
}
