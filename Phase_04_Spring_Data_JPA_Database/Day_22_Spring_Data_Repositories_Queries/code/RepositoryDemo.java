package code;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Driver class demonstrating Day 22: Spring Data Repositories & Query Methods.
 *
 * Demonstrates:
 * 1. Automatic Query Derivation from method names.
 * 2. Parameterized custom JPQL aggregation queries.
 * 3. Page<T> vs Slice<T> pagination performance trade-offs for AI chat context.
 */
public class RepositoryDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 22: SPRING DATA REPOSITORIES, METHOD DERIVATION, JPQL & PAGINATION         ");
        System.out.println("================================================================================");

        DynamicQuerySimulator repo = new DynamicQuerySimulator();

        // Populate sample chat history for session "sess_ai_101"
        Instant now = Instant.now();
        repo.saveAll(List.of(
            new ChatMessageEntity(1L, "sess_ai_101", "SYSTEM", "You are an expert Spring Boot AI architect.", 14, now.minus(50, ChronoUnit.SECONDS)),
            new ChatMessageEntity(2L, "sess_ai_101", "USER", "How does Spring Data generate queries from method names?", 11, now.minus(40, ChronoUnit.SECONDS)),
            new ChatMessageEntity(3L, "sess_ai_101", "ASSISTANT", "Spring Data parses the method name into a query syntax tree and generates SQL via dynamic proxy.", 19, now.minus(30, ChronoUnit.SECONDS)),
            new ChatMessageEntity(4L, "sess_ai_101", "USER", "What is the difference between Page and Slice?", 9, now.minus(20, ChronoUnit.SECONDS)),
            new ChatMessageEntity(5L, "sess_ai_101", "ASSISTANT", "Page executes a count query to know total pages; Slice queries limit+1 with zero count query.", 18, now.minus(10, ChronoUnit.SECONDS)),
            new ChatMessageEntity(6L, "sess_ai_202", "USER", "Hello from another session", 5, now)
        ));

        // -------------------------------------------------------------------------
        // SCENARIO 1: METHOD NAME QUERY DERIVATION
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: Method Name Derived Query (Chronological Chat History) ---");
        List<ChatMessageEntity> history = repo.findBySessionIdOrderByCreatedAtAsc("sess_ai_101");
        System.out.println(" Retrieved " + history.size() + " messages for session 'sess_ai_101':");
        history.forEach(m -> System.out.println("   " + m));

        // -------------------------------------------------------------------------
        // SCENARIO 2: MULTI-CRITERIA FILTER (Role = USER)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: Multi-Criteria Derived Query (Only USER Messages) ---");
        List<ChatMessageEntity> userMessages = repo.findBySessionIdAndRole("sess_ai_101", "USER");
        System.out.println(" Found " + userMessages.size() + " user messages:");
        userMessages.forEach(m -> System.out.println("   " + m));

        // -------------------------------------------------------------------------
        // SCENARIO 3: CUSTOM JPQL AGGREGATION QUERY
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: Custom JPQL Aggregation (Token Sum by Session) ---");
        int totalTokens = repo.sumTokensBySessionId("sess_ai_101");
        long messageCount = repo.countBySessionId("sess_ai_101");
        System.out.println(" Session 'sess_ai_101' Stats: Total Messages = " + messageCount + ", Total Tokens Consumed = " + totalTokens);

        // -------------------------------------------------------------------------
        // SCENARIO 4: STANDARD Page<T> PAGINATION (With COUNT(*) query)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 4: Standard Page<T> Pagination (Page size = 2) ---");
        DynamicQuerySimulator.PageRequest pageReq0 = new DynamicQuerySimulator.PageRequest(0, 2);
        DynamicQuerySimulator.Page<ChatMessageEntity> page0 = repo.findBySessionId("sess_ai_101", pageReq0);
        System.out.println(" Page 0 Results:");
        System.out.println("   Total Elements: " + page0.totalElements());
        System.out.println("   Total Pages: " + page0.totalPages());
        System.out.println("   Has Next Page? " + page0.hasNext());
        page0.content().forEach(m -> System.out.println("     " + m));

        // -------------------------------------------------------------------------
        // SCENARIO 5: OPTIMIZED Slice<T> PAGINATION (Zero COUNT(*) query overhead)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 5: High-Performance Slice<T> Pagination (Zero COUNT(*) Overhead) ---");
        DynamicQuerySimulator.Slice<ChatMessageEntity> slice0 = repo.findSliceBySessionId("sess_ai_101", pageReq0);
        System.out.println(" Slice 0 Results (Ideal for AI chat infinite scroll):");
        System.out.println("   Items in slice: " + slice0.content().size());
        System.out.println("   Has Next Page (without executing COUNT(*))? " + slice0.hasNext());
        slice0.content().forEach(m -> System.out.println("     " + m));

        System.out.println("\n================================================================================");
        System.out.println(" DAY 22 DEMONSTRATION COMPLETE: REPOSITORY QUERY PATTERNS VERIFIED!             ");
        System.out.println("================================================================================");
    }
}
