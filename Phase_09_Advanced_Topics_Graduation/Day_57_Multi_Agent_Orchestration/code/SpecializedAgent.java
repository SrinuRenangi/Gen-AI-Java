package com.genai.enterprise.multiagent;

/**
 * Interface and default implementations for specialized worker agents.
 */
public interface SpecializedAgent {

    AgentRole getRole();
    void execute(SharedAgentWorkspace workspace);

    // 1. Researcher Agent Implementation
    static SpecializedAgent createResearcher() {
        return new SpecializedAgent() {
            @Override
            public AgentRole getRole() { return AgentRole.RESEARCHER; }

            @Override
            public void execute(SharedAgentWorkspace ws) {
                simulateWork(40);
                String research = """
                    ARCHITECTURAL RESEARCH FINDINGS:
                    - Token Bucket algorithm selected for predictable burst tolerance.
                    - Recommendation: Use AtomicLong with epoch-millisecond delta refill.
                    - Concurrency: Lock-free CAS (compare-and-swap) preferred over synchronized blocks.
                    """;
                ws.putArtifact("research_findings", research);
                ws.logMessage(AgentRole.RESEARCHER, AgentRole.SUPERVISOR, 
                        "Completed algorithm research: Recommended CAS-based Token Bucket.");
            }
        };
    }

    // 2. Coder Agent Implementation
    static SpecializedAgent createCoder() {
        return new SpecializedAgent() {
            @Override
            public AgentRole getRole() { return AgentRole.CODER; }

            @Override
            public void execute(SharedAgentWorkspace ws) {
                simulateWork(60);
                String research = ws.getArtifact("research_findings");
                String code = String.format("""
                    public class CasTokenBucketRateLimiter {
                        private final long capacity;
                        private final AtomicLong tokens;
                        public CasTokenBucketRateLimiter(long capacity) {
                            this.capacity = capacity;
                            this.tokens = new AtomicLong(capacity);
                        }
                        public boolean tryAcquire() {
                            return tokens.getAndUpdate(t -> t > 0 ? t - 1 : 0) > 0;
                        }
                    }
                    // Generated based on: %s
                    """, (research != null ? "Research Guideline Verified" : "Default Template"));
                ws.putArtifact("source_code", code);
                ws.logMessage(AgentRole.CODER, AgentRole.SUPERVISOR, 
                        "Implemented CasTokenBucketRateLimiter in Java 21 adhering to research guidelines.");
            }
        };
    }

    // 3. Security Auditor Agent Implementation
    static SpecializedAgent createSecurityAuditor() {
        return new SpecializedAgent() {
            @Override
            public AgentRole getRole() { return AgentRole.SECURITY_AUDITOR; }

            @Override
            public void execute(SharedAgentWorkspace ws) {
                simulateWork(35);
                String code = ws.getArtifact("source_code");
                boolean hasAtomic = code != null && code.contains("AtomicLong");
                String auditReport = hasAtomic 
                        ? "AUDIT PASSED: Memory visibility safe via AtomicLong. No race conditions detected."
                        : "AUDIT FAILED: Non-thread-safe mutation detected.";
                ws.putArtifact("security_audit", auditReport);
                ws.logMessage(AgentRole.SECURITY_AUDITOR, AgentRole.SUPERVISOR, auditReport);
            }
        };
    }

    private static void simulateWork(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
