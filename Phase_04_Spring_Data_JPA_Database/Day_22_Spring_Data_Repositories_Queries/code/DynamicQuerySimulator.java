package code;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Enterprise Spring Data Repository Simulator.
 *
 * Demonstrates:
 * 1. Method Name Query Derivation: Spring Data parsing method signatures into executable queries.
 * 2. Custom @Query (JPQL): Parameterized aggregation (e.g. SUM(tokenCount)).
 * 3. Page<T> vs Slice<T>: Explaining why Slice<T> is superior for infinite-scroll AI chat histories.
 */
public class DynamicQuerySimulator {

    private final List<ChatMessageEntity> database = new ArrayList<>();

    public record Page<T>(List<T> content, int pageNumber, int pageSize, long totalElements, int totalPages) {
        public boolean hasNext() { return pageNumber < totalPages - 1; }
    }

    public record Slice<T>(List<T> content, int pageNumber, int pageSize, boolean hasNext) {}

    public record PageRequest(int page, int size) {}

    public void save(ChatMessageEntity entity) {
        database.add(entity);
    }

    public void saveAll(List<ChatMessageEntity> entities) {
        database.addAll(entities);
    }

    public Optional<ChatMessageEntity> findById(Long id) {
        return database.stream().filter(m -> Objects.equals(m.getId(), id)).findFirst();
    }

    public List<ChatMessageEntity> findAll() {
        return List.copyOf(database);
    }

    // -------------------------------------------------------------------------
    // 1. METHOD NAME DERIVATION: findBySessionIdOrderByCreatedAtAsc
    // -------------------------------------------------------------------------
    public List<ChatMessageEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId) {
        System.out.println("  [Spring Data Derived Query] Executing: SELECT * FROM chat_messages WHERE session_id = '" 
            + sessionId + "' ORDER BY created_at ASC");
        return database.stream()
            .filter(m -> Objects.equals(m.getSessionId(), sessionId))
            .sorted(Comparator.comparing(ChatMessageEntity::getCreatedAt))
            .toList();
    }

    // -------------------------------------------------------------------------
    // 2. METHOD NAME DERIVATION: findBySessionIdAndRole
    // -------------------------------------------------------------------------
    public List<ChatMessageEntity> findBySessionIdAndRole(String sessionId, String role) {
        System.out.println("  [Spring Data Derived Query] Executing: SELECT * FROM chat_messages WHERE session_id = '" 
            + sessionId + "' AND role = '" + role + "'");
        return database.stream()
            .filter(m -> Objects.equals(m.getSessionId(), sessionId) && Objects.equals(m.getRole(), role))
            .toList();
    }

    // -------------------------------------------------------------------------
    // 3. METHOD NAME DERIVATION: findByTokenCountBetween
    // -------------------------------------------------------------------------
    public List<ChatMessageEntity> findByTokenCountBetween(int minTokens, int maxTokens) {
        System.out.println("  [Spring Data Derived Query] Executing: SELECT * FROM chat_messages WHERE token_count BETWEEN " 
            + minTokens + " AND " + maxTokens);
        return database.stream()
            .filter(m -> m.getTokenCount() >= minTokens && m.getTokenCount() <= maxTokens)
            .toList();
    }

    // -------------------------------------------------------------------------
    // 4. METHOD NAME DERIVATION: countBySessionId
    // -------------------------------------------------------------------------
    public long countBySessionId(String sessionId) {
        System.out.println("  [Spring Data Derived Query] Executing: SELECT COUNT(*) FROM chat_messages WHERE session_id = '" + sessionId + "'");
        return database.stream()
            .filter(m -> Objects.equals(m.getSessionId(), sessionId))
            .count();
    }

    // -------------------------------------------------------------------------
    // 5. CUSTOM @Query (JPQL): Aggregate token sum
    // -------------------------------------------------------------------------
    public int sumTokensBySessionId(String sessionId) {
        System.out.println("  [Custom @Query JPQL] Executing: SELECT COALESCE(SUM(m.tokenCount), 0) FROM ChatMessageEntity m WHERE m.sessionId = :sessionId");
        return database.stream()
            .filter(m -> Objects.equals(m.getSessionId(), sessionId))
            .mapToInt(ChatMessageEntity::getTokenCount)
            .sum();
    }

    // -------------------------------------------------------------------------
    // 6. PAGINATION: Page<T> (With COUNT(*) query)
    // -------------------------------------------------------------------------
    public Page<ChatMessageEntity> findBySessionId(String sessionId, PageRequest pageRequest) {
        System.out.println("  [Spring Data Page<T>] Query 1: SELECT * FROM chat_messages WHERE session_id = '" 
            + sessionId + "' LIMIT " + pageRequest.size() + " OFFSET " + (pageRequest.page() * pageRequest.size()));
        System.out.println("  [Spring Data Page<T>] Query 2 (Heavy!): SELECT COUNT(*) FROM chat_messages WHERE session_id = '" + sessionId + "'");

        List<ChatMessageEntity> matches = database.stream()
            .filter(m -> Objects.equals(m.getSessionId(), sessionId))
            .sorted(Comparator.comparing(ChatMessageEntity::getCreatedAt).reversed())
            .toList();

        long totalElements = matches.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());

        int start = pageRequest.page() * pageRequest.size();
        int end = Math.min(start + pageRequest.size(), matches.size());
        List<ChatMessageEntity> content = (start < matches.size()) ? matches.subList(start, end) : List.of();

        return new Page<>(content, pageRequest.page(), pageRequest.size(), totalElements, totalPages);
    }

    // -------------------------------------------------------------------------
    // 7. PAGINATION: Slice<T> (Without COUNT(*) query — queries LIMIT size + 1)
    // -------------------------------------------------------------------------
    public Slice<ChatMessageEntity> findSliceBySessionId(String sessionId, PageRequest pageRequest) {
        int limitPlusOne = pageRequest.size() + 1;
        System.out.println("  [Spring Data Slice<T>] Optimized Single Query (ZERO COUNT(*)): SELECT * FROM chat_messages WHERE session_id = '" 
            + sessionId + "' LIMIT " + limitPlusOne + " OFFSET " + (pageRequest.page() * pageRequest.size()));

        List<ChatMessageEntity> matches = database.stream()
            .filter(m -> Objects.equals(m.getSessionId(), sessionId))
            .sorted(Comparator.comparing(ChatMessageEntity::getCreatedAt).reversed())
            .toList();

        int start = pageRequest.page() * pageRequest.size();
        int end = Math.min(start + limitPlusOne, matches.size());
        List<ChatMessageEntity> rawList = (start < matches.size()) ? matches.subList(start, end) : List.of();

        boolean hasNext = rawList.size() > pageRequest.size();
        List<ChatMessageEntity> content = hasNext ? rawList.subList(0, pageRequest.size()) : rawList;

        return new Slice<>(content, pageRequest.page(), pageRequest.size(), hasNext);
    }
}
