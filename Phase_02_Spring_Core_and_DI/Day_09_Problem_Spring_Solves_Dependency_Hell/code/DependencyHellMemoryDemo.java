package com.javagenai.day09.coupling_and_memory;

import java.util.Objects;

/**
 * DAY 09: COMPREHENSIVE DEPENDENCY HELL, HEAP MEMORY, AND TESTABILITY DEMONSTRATION
 *
 * This runnable class illustrates:
 * 1. Tight Coupling & Object Proliferation: Invoking 'new' creates duplicate heap instances,
 *    inflating the memory footprint and causing GC allocation churn.
 * 2. Shared Singleton Reference Wiring (IoC Pattern): Injected singletons share identical heap
 *    pointers across thousands of requests with zero auxiliary object allocations.
 * 3. Zero-Friction Testability: Constructor injection allows instant stubbing/mocking without
 *    bytecode manipulation or external network connections.
 */
public class DependencyHellMemoryDemo {

    // =========================================================================
    // 1. ABSTRACTIONS (INTERFACES)
    // =========================================================================
    public interface EmbeddingClient {
        float[] generateEmbedding(String text);
        String getProviderName();
    }

    public interface VectorRepository {
        void persistVector(String id, float[] vector);
        int getStoredCount();
    }

    // =========================================================================
    // 2. CONCRETE PRODUCTION IMPLEMENTATIONS
    // =========================================================================
    public static class OpenAiEmbeddingClient implements EmbeddingClient {
        private final byte[] internalBuffer = new byte[1024]; // Simulating internal heap buffers

        @Override
        public float[] generateEmbedding(String text) {
            // Simulated 4-dimensional embedding vector
            return new float[] { 0.12f, -0.45f, 0.88f, 0.03f };
        }

        @Override
        public String getProviderName() {
            return "OpenAI-text-embedding-3-small";
        }
    }

    public static class PostgresVectorRepository implements VectorRepository {
        private final String connectionUrl;
        private int storedCount = 0;

        public PostgresVectorRepository(String connectionUrl) {
            this.connectionUrl = Objects.requireNonNull(connectionUrl, "connectionUrl must not be null");
        }

        @Override
        public void persistVector(String id, float[] vector) {
            storedCount++;
        }

        @Override
        public int getStoredCount() {
            return storedCount;
        }
    }

    // =========================================================================
    // 3. TIGHTLY COUPLED ANTI-PATTERN (HARDCODED 'new')
    // =========================================================================
    public static class TightlyCoupledRagService {
        // Hardcoded dependencies: No interfaces, hardcoded configuration
        private final OpenAiEmbeddingClient embeddingClient;
        private final PostgresVectorRepository vectorRepository;

        public TightlyCoupledRagService() {
            // ANTI-PATTERN: Direct instantiation via 'new'
            // Every instance of this service creates new internal instances!
            this.embeddingClient = new OpenAiEmbeddingClient();
            this.vectorRepository = new PostgresVectorRepository("jdbc:postgresql://prod-db:5432/vectors");
        }

        public void indexDocument(String docId, String content) {
            float[] vector = embeddingClient.generateEmbedding(content);
            vectorRepository.persistVector(docId, vector);
        }

        public int getEmbeddingClientIdentity() {
            return System.identityHashCode(embeddingClient);
        }

        public int getRepositoryIdentity() {
            return System.identityHashCode(vectorRepository);
        }
    }

    // =========================================================================
    // 4. INVERSION OF CONTROL / CONSTRUCTOR INJECTION PATTERN
    // =========================================================================
    public static class DecoupledRagService {
        // Clean abstraction: Depends ONLY on interfaces, immutable via final
        private final EmbeddingClient embeddingClient;
        private final VectorRepository vectorRepository;

        // Constructor Injection: Dependencies are handed in by the external assembler (IoC container)
        public DecoupledRagService(EmbeddingClient embeddingClient, VectorRepository vectorRepository) {
            this.embeddingClient = Objects.requireNonNull(embeddingClient, "embeddingClient must not be null");
            this.vectorRepository = Objects.requireNonNull(vectorRepository, "vectorRepository must not be null");
        }

        public void indexDocument(String docId, String content) {
            float[] vector = embeddingClient.generateEmbedding(content);
            vectorRepository.persistVector(docId, vector);
        }

        public int getEmbeddingClientIdentity() {
            return System.identityHashCode(embeddingClient);
        }

        public int getRepositoryIdentity() {
            return System.identityHashCode(vectorRepository);
        }
    }

    // =========================================================================
    // 5. TEST STUBS / MOCKS FOR ZERO-FRICTION UNIT TESTING
    // =========================================================================
    public static class StubEmbeddingClient implements EmbeddingClient {
        @Override
        public float[] generateEmbedding(String text) {
            return new float[] { 1.0f, 1.0f, 1.0f, 1.0f }; // Canned deterministic vector
        }

        @Override
        public String getProviderName() {
            return "Deterministic-Test-Stub";
        }
    }

    public static class InMemoryVectorRepository implements VectorRepository {
        private int count = 0;

        @Override
        public void persistVector(String id, float[] vector) {
            count++;
        }

        @Override
        public int getStoredCount() {
            return count;
        }
    }

    // =========================================================================
    // MAIN ENTRY POINT & EXPERIMENTS
    // =========================================================================
    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 09: DEPENDENCY HELL, HEAP ALLOCATIONS & INVERSION OF CONTROL EXPERIMENT   ");
        System.out.println("================================================================================");

        // ---------------------------------------------------------------------
        // EXPERIMENT 1: Heap Proliferation with Hardcoded 'new'
        // ---------------------------------------------------------------------
        System.out.println("\n--- [EXPERIMENT 1] Tightly Coupled Services with 'new' ---");
        System.out.println("Creating 3 TightlyCoupledRagService instances (e.g., across 3 web requests)...");

        TightlyCoupledRagService coupled1 = new TightlyCoupledRagService();
        TightlyCoupledRagService coupled2 = new TightlyCoupledRagService();
        TightlyCoupledRagService coupled3 = new TightlyCoupledRagService();

        System.out.printf("  coupled1 -> EmbeddingClient Heap ID: 0x%08X | Repository Heap ID: 0x%08X%n",
                coupled1.getEmbeddingClientIdentity(), coupled1.getRepositoryIdentity());
        System.out.printf("  coupled2 -> EmbeddingClient Heap ID: 0x%08X | Repository Heap ID: 0x%08X%n",
                coupled2.getEmbeddingClientIdentity(), coupled2.getRepositoryIdentity());
        System.out.printf("  coupled3 -> EmbeddingClient Heap ID: 0x%08X | Repository Heap ID: 0x%08X%n",
                coupled3.getEmbeddingClientIdentity(), coupled3.getRepositoryIdentity());

        System.out.println("  => OBSERVATION: Every service instance created redundant duplicate objects on the Heap!");
        System.out.println("     In a high-throughput server (e.g., 10,000 req/sec), this causes rapid Eden space fill");
        System.out.println("     and severe GC thrashing (premature promotion and Stop-The-World minor GC pauses).");

        // ---------------------------------------------------------------------
        // EXPERIMENT 2: Shared Singletons via Inversion of Control
        // ---------------------------------------------------------------------
        System.out.println("\n--- [EXPERIMENT 2] Decoupled Services with Shared Managed Singletons ---");
        System.out.println("Simulating an IoC Container creating Singletons ONCE and injecting shared references...");

        // Container creates dependencies once (Singleton Scope)
        EmbeddingClient sharedEmbeddingClient = new OpenAiEmbeddingClient();
        VectorRepository sharedVectorRepo = new PostgresVectorRepository("jdbc:postgresql://prod-db:5432/vectors");

        // Service instances receive the exact same references via Constructor Injection
        DecoupledRagService decoupled1 = new DecoupledRagService(sharedEmbeddingClient, sharedVectorRepo);
        DecoupledRagService decoupled2 = new DecoupledRagService(sharedEmbeddingClient, sharedVectorRepo);
        DecoupledRagService decoupled3 = new DecoupledRagService(sharedEmbeddingClient, sharedVectorRepo);

        System.out.printf("  decoupled1 -> EmbeddingClient Heap ID: 0x%08X | Repository Heap ID: 0x%08X%n",
                decoupled1.getEmbeddingClientIdentity(), decoupled1.getRepositoryIdentity());
        System.out.printf("  decoupled2 -> EmbeddingClient Heap ID: 0x%08X | Repository Heap ID: 0x%08X%n",
                decoupled2.getEmbeddingClientIdentity(), decoupled2.getRepositoryIdentity());
        System.out.printf("  decoupled3 -> EmbeddingClient Heap ID: 0x%08X | Repository Heap ID: 0x%08X%n",
                decoupled3.getEmbeddingClientIdentity(), decoupled3.getRepositoryIdentity());

        boolean sameClients = (decoupled1.getEmbeddingClientIdentity() == decoupled2.getEmbeddingClientIdentity())
                && (decoupled2.getEmbeddingClientIdentity() == decoupled3.getEmbeddingClientIdentity());
        System.out.println("  => OBSERVATION: Shared heap pointer verified? " + sameClients);
        System.out.println("     All services point to the SAME long-lived singleton instances on the Heap.");
        System.out.println("     Memory consumption per service is reduced to a tiny 8-byte reference pointer!");

        // ---------------------------------------------------------------------
        // EXPERIMENT 3: Zero-Friction Testability (Zero Bytecode Mocking)
        // ---------------------------------------------------------------------
        System.out.println("\n--- [EXPERIMENT 3] Zero-Friction Unit Testing ---");
        System.out.println("Testing DecoupledRagService with in-memory test doubles (no network, no database):");

        InMemoryVectorRepository testRepo = new InMemoryVectorRepository();
        StubEmbeddingClient testStubClient = new StubEmbeddingClient();

        DecoupledRagService testService = new DecoupledRagService(testStubClient, testRepo);
        testService.indexDocument("doc-101", "The quick brown fox jumps over the lazy dog");

        System.out.println("  Document indexed successfully in test environment!");
        System.out.println("  Persisted vectors in test repository: " + testRepo.getStoredCount());
        assert testRepo.getStoredCount() == 1 : "Expected 1 stored vector in test repo";
        System.out.println("  => Unit test passed with 0ms network latency and $0.00 API cost.");

        System.out.println("\n================================================================================");
        System.out.println(" EXPERIMENT COMPLETE: IoC ensures clean separation, minimal heap bloat & testability!");
        System.out.println("================================================================================");
    }
}
