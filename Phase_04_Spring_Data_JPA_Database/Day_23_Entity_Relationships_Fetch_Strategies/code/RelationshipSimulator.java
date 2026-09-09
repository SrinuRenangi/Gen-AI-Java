package code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Relationship & Fetch Strategy Simulator.
 *
 * Simulates:
 * 1. The N+1 Query Disaster: Lazy fetching child collections inside a loop.
 * 2. The JOIN FETCH / @EntityGraph Solution: Fetching parent + children in a single SQL query.
 * 3. CascadeType.ALL: Persisting parent automatically saves children.
 * 4. orphanRemoval = true: Removing child from parent list emits SQL DELETE.
 */
public class RelationshipSimulator {

    private final Map<Long, SessionEntity> sessionsDb = new HashMap<>();
    private final Map<Long, List<MessageEntity>> messagesDb = new HashMap<>();
    private final List<String> executedSqlQueries = new ArrayList<>();

    public RelationshipSimulator() {
        // Pre-populate 3 sessions, each with 2 messages
        for (long sId = 1; sId <= 3; sId++) {
            SessionEntity s = new SessionEntity(sId, "AI Chat Session " + sId, "user_" + sId);
            sessionsDb.put(sId, s);

            List<MessageEntity> msgs = new ArrayList<>();
            MessageEntity m1 = new MessageEntity(sId * 10 + 1, "USER", "Prompt " + sId, 10);
            MessageEntity m2 = new MessageEntity(sId * 10 + 2, "ASSISTANT", "Completion " + sId, 25);
            m1.setSession(s);
            m2.setSession(s);
            msgs.add(m1);
            msgs.add(m2);
            messagesDb.put(sId, msgs);
        }
    }

    /**
     * SIMULATES THE N+1 QUERY PROBLEM:
     * Step 1: SELECT * FROM sessions (1 query)
     * Step 2: For each session, lazy proxy fires: SELECT * FROM messages WHERE session_id = ? (N queries)
     */
    public List<SessionEntity> findAllWithLazyNPlusOne() {
        executedSqlQueries.clear();

        // 1 Initial Query for Parents
        String initialQuery = "SELECT * FROM conversation_sessions;";
        executedSqlQueries.add(initialQuery);
        System.out.println("  [SQL 1 (Initial)] " + initialQuery);

        List<SessionEntity> results = new ArrayList<>();
        for (SessionEntity rawSession : sessionsDb.values()) {
            SessionEntity s = new SessionEntity(rawSession.getId(), rawSession.getTitle(), rawSession.getUserId());
            results.add(s);
        }

        // Now simulate user code looping through sessions to read messages
        System.out.println("  --> Looping through " + results.size() + " sessions to inspect messages:");
        for (SessionEntity s : results) {
            String childQuery = "SELECT * FROM chat_messages WHERE session_id = " + s.getId() + ";";
            executedSqlQueries.add(childQuery);
            System.out.println("    [SQL LAZY PROXY] " + childQuery);

            List<MessageEntity> children = messagesDb.get(s.getId());
            for (MessageEntity m : children) {
                s.addMessage(m);
            }
        }

        return results;
    }

    /**
     * SIMULATES THE JOIN FETCH / @EntityGraph SOLUTION:
     * Emits EXACTLY 1 query with an SQL INNER/LEFT JOIN!
     */
    public List<SessionEntity> findAllWithJoinFetch() {
        executedSqlQueries.clear();

        String joinedQuery = "SELECT s.*, m.* FROM conversation_sessions s LEFT JOIN chat_messages m ON s.id = m.session_id;";
        executedSqlQueries.add(joinedQuery);
        System.out.println("  [SQL 1 (JOIN FETCH)] " + joinedQuery);

        Map<Long, SessionEntity> hydrated = new HashMap<>();
        for (SessionEntity rawSession : sessionsDb.values()) {
            SessionEntity s = hydrated.computeIfAbsent(
                rawSession.getId(),
                id -> new SessionEntity(id, rawSession.getTitle(), rawSession.getUserId())
            );
            List<MessageEntity> children = messagesDb.get(s.getId());
            for (MessageEntity m : children) {
                s.addMessage(m);
            }
        }

        return new ArrayList<>(hydrated.values());
    }

    /**
     * SIMULATES CASCADE PERSIST:
     * Persisting parent automatically cascades INSERT statements for all child messages.
     */
    public void persistWithCascade(SessionEntity session) {
        executedSqlQueries.clear();
        String parentSql = "INSERT INTO conversation_sessions (id, title, user_id) VALUES (" 
            + session.getId() + ", '" + session.getTitle() + "', '" + session.getUserId() + "');";
        executedSqlQueries.add(parentSql);
        System.out.println("  [CASCADE SQL 1] " + parentSql);

        for (MessageEntity child : session.getMessages()) {
            String childSql = "INSERT INTO chat_messages (id, session_id, role, content, token_count) VALUES ("
                + child.getId() + ", " + session.getId() + ", '" + child.getRole() + "', '" 
                + child.getContent() + "', " + child.getTokenCount() + ");";
            executedSqlQueries.add(childSql);
            System.out.println("  [CASCADE SQL CHILD] " + childSql);
        }
    }

    public List<String> getExecutedSqlQueries() {
        return List.copyOf(executedSqlQueries);
    }
}
