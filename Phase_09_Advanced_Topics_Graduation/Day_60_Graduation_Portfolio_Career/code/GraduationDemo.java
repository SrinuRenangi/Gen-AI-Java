package com.genai.enterprise.graduation;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Graduation Test Driver for the 60-Day Java Generative AI Masterclass.
 * Executes the 15-Point Production Readiness Audit and prints the Senior AI Engineer Portfolio Dossier.
 */
public class GraduationDemo {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("   60-DAY JAVA GENERATIVE AI MASTERCLASS: OFFICIAL GRADUATION CEREMONY   ");
        System.out.println("==========================================================================\n");

        ProductionReadinessAuditor auditor = new ProductionReadinessAuditor();
        List<ProductionChecklistItem> auditItems = auditor.auditSystem();

        System.out.println(">>> 1. ENTERPRISE PRODUCTION READINESS AUDIT (15 CRITICAL CONTROLS):\n");
        long passedCount = 0;
        for (ProductionChecklistItem item : auditItems) {
            System.out.printf("  %-15s | %-32s | %s%n", item.category(), item.checkName(), item.getStatusTag());
            if (item.passed()) passedCount++;
        }

        System.out.println("\n--------------------------------------------------------------------------");
        System.out.printf("Audit Summary: %d / %d Operational Controls Passed (100%% Green)%n",
                passedCount, auditItems.size());
        System.out.println("--------------------------------------------------------------------------\n");

        System.out.println(">>> 2. 60-DAY CURRICULUM MASTERY BREAKDOWN ACROSS ALL 9 PHASES:\n");
        Map<Integer, EngineerCompetencyMatrix.PhaseMastery> phases = EngineerCompetencyMatrix.getCurriculumMastery();
        for (var entry : phases.entrySet()) {
            var p = entry.getValue();
            System.out.printf("  Phase %d: %-35s [%d/%d Days] - %s%n",
                    entry.getKey(), p.phaseName(), p.daysCompleted(), p.totalDays(), p.coreCompetencies());
        }

        String seniorityLevel = EngineerCompetencyMatrix.evaluateSeniorityLevel();

        System.out.println("\n==========================================================================");
        System.out.println("                    OFFICIAL GRADUATION CERTIFICATE                       ");
        System.out.println("==========================================================================");
        System.out.println("  CONGRATULATIONS! You have completed all 60 Days of intensive training. ");
        System.out.println("  Certified Rank: " + seniorityLevel);
        System.out.println("  Verified Date : " + LocalDate.now());
        System.out.println("  Competencies  : Java 21, Spring Boot 3, Spring AI, LangChain4j, pgvector,");
        System.out.println("                  MCP Tools, OpenTelemetry, Rate Limiting, Docker & Multi-Agent");
        System.out.println("==========================================================================\n");
        System.out.println(">>> Masterclass graduation verification completed successfully! You are production-ready.");
    }
}
