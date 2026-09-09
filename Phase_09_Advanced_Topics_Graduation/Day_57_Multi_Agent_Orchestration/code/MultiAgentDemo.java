package com.genai.enterprise.multiagent;

/**
 * Verification test driver for Multi-Agent Orchestration (Supervisor Pattern) in Java 21.
 */
public class MultiAgentDemo {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  DAY 57: MULTI-AGENT HIERARCHICAL ORCHESTRATION IN JAVA 21 (VIRTUAL THREADS)");
        System.out.println("==========================================================================\n");

        SupervisorOrchestrator supervisor = new SupervisorOrchestrator();
        String goal = "Design, Implement, and Security Audit a High-Throughput Token Bucket Rate Limiter";

        System.out.println("[Supervisor] Received Mission Goal: " + goal + "\n");
        SharedAgentWorkspace workspace = supervisor.orchestrateMission(goal);

        System.out.println("--------------------------------------------------------------------------");
        System.out.println("                INTER-AGENT MESSAGE DISPATCH LOG                          ");
        System.out.println("--------------------------------------------------------------------------");
        for (AgentMessage msg : workspace.getMessageLog()) {
            System.out.printf("[%s -> %s]: %s%n",
                    msg.sender().getTitle(), msg.recipient().getTitle(), msg.content());
        }

        System.out.println("\n--------------------------------------------------------------------------");
        System.out.println("                      SYNTHESIZED ARTIFACTS                               ");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("1. [RESEARCH ARTIFACT]:\n" + workspace.getArtifact("research_findings"));
        System.out.println("2. [CODE ARTIFACT]:\n" + workspace.getArtifact("source_code"));
        System.out.println("3. [SECURITY AUDIT ARTIFACT]:\n" + workspace.getArtifact("security_audit"));

        System.out.println("==========================================================================");
        System.out.println("Mission Approval Status : " + (workspace.isApproved() ? "APPROVED (100% Consensus)" : "REJECTED"));
        System.out.println("==========================================================================");
        System.out.println(">>> Multi-agent orchestration verification completed successfully!");
    }
}
