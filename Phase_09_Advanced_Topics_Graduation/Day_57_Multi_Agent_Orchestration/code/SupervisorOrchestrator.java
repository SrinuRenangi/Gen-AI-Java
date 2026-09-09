package com.genai.enterprise.multiagent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Hierarchical Supervisor Orchestrator managing task decomposition,
 * specialist delegation via Java 21 Virtual Threads, and quality consensus.
 */
public class SupervisorOrchestrator {

    private final SpecializedAgent researcher = SpecializedAgent.createResearcher();
    private final SpecializedAgent coder = SpecializedAgent.createCoder();
    private final SpecializedAgent auditor = SpecializedAgent.createSecurityAuditor();

    public SharedAgentWorkspace orchestrateMission(String missionGoal) {
        SharedAgentWorkspace workspace = new SharedAgentWorkspace(missionGoal);
        workspace.logMessage(AgentRole.SUPERVISOR, AgentRole.SUPERVISOR, 
                "Initiating Mission: '" + missionGoal + "'");

        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Step 1: Delegate Architecture Research
            workspace.logMessage(AgentRole.SUPERVISOR, AgentRole.RESEARCHER, 
                    "Task: Research optimal concurrency algorithms for goal.");
            Future<?> f1 = virtualExecutor.submit(() -> researcher.execute(workspace));
            f1.get();

            // Step 2: Delegate Implementation to Coder
            workspace.logMessage(AgentRole.SUPERVISOR, AgentRole.CODER, 
                    "Task: Implement production Java 21 class based on research.");
            Future<?> f2 = virtualExecutor.submit(() -> coder.execute(workspace));
            f2.get();

            // Step 3: Delegate Security Audit
            workspace.logMessage(AgentRole.SUPERVISOR, AgentRole.SECURITY_AUDITOR, 
                    "Task: Audit generated code for concurrency safety and vulnerabilities.");
            Future<?> f3 = virtualExecutor.submit(() -> auditor.execute(workspace));
            f3.get();

            // Step 4: Supervisor Evaluation & Consensus
            String audit = workspace.getArtifact("security_audit");
            if (audit != null && audit.contains("AUDIT PASSED")) {
                workspace.setApproved(true);
                workspace.logMessage(AgentRole.SUPERVISOR, AgentRole.SUPERVISOR, 
                        "CONSENSUS REACHED: All subtasks satisfied. Artifact signed off.");
            } else {
                workspace.setApproved(false);
                workspace.logMessage(AgentRole.SUPERVISOR, AgentRole.SUPERVISOR, 
                        "CONSENSUS FAILED: Auditor flagged issues. Revision required.");
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Multi-agent orchestration interrupted", e);
        }

        return workspace;
    }
}
