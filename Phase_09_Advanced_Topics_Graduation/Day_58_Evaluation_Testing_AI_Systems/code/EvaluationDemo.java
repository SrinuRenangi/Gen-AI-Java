package com.genai.enterprise.evaluation;

import java.util.List;

/**
 * Verification test driver for RAG Triad Evaluation & LLM-as-a-Judge in Java 21.
 */
public class EvaluationDemo {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("     DAY 58: RAG TRIAD EVALUATION & AUTOMATED AI TESTING IN JAVA 21       ");
        System.out.println("==========================================================================\n");

        EvaluationSuite suite = new EvaluationSuite();
        LlmAsAJudgeEvaluator judge = suite.getEvaluator();

        // Test Case 1: High Quality Grounded Response
        RagTestCase tc1 = new RagTestCase(
                "TC-001",
                "What is the company vacation policy for senior engineers?",
                "Corporate HR Policy Section 8: Senior engineers receive 25 paid vacation days per calendar year.",
                "Senior engineers receive 25 paid vacation days per calendar year according to HR policy.",
                "25 paid vacation days per calendar year"
        );
        suite.addTestCase(tc1);

        // Test Case 2: Hallucinated Response (Violates Groundedness)
        RagTestCase tc2 = new RagTestCase(
                "TC-002",
                "What is the interest rate on the premier corporate savings account?",
                "Banking Product Guide: Premier Corporate Savings earns a variable interest rate of 4.25% APY.",
                "The premier corporate savings account earns a guaranteed 100% risk-free 12.5% return with daily dividends.",
                "4.25% APY variable"
        );
        suite.addTestCase(tc2);

        // Test Case 3: Irrelevant Retrieval (Violates Context Relevance)
        RagTestCase tc3 = new RagTestCase(
                "TC-003",
                "How do I configure Spring Boot OAuth2 security?",
                "Cafeteria Lunch Menu: Tuesday special includes chicken parmigiana and vegetable soup.",
                "To configure Spring Boot OAuth2 security, define a SecurityFilterChain bean with oauth2Login.",
                "Configure SecurityFilterChain with oauth2Login()"
        );
        suite.addTestCase(tc3);

        // Run Individual Diagnostics
        System.out.println(">>> INDIVIDUAL TEST CASE DIAGNOSTICS:\n");
        for (RagTestCase tc : List.of(tc1, tc2, tc3)) {
            RagTriadMetrics m = judge.evaluate(tc);
            System.out.println("[" + tc.testId() + "] Query: '" + tc.userQuery() + "'");
            System.out.printf("  Context Relevance : %.2f | %s%n", m.contextRelevance(), m.contextRationale());
            System.out.printf("  Groundedness      : %.2f | %s%n", m.groundedness(), m.groundednessRationale());
            System.out.printf("  Answer Relevance  : %.2f | %s%n", m.answerRelevance(), m.answerRationale());
            System.out.printf("  Harmonic Composite: %.2f%n%n", m.getCompositeScore());
        }

        // Run CI/CD Cohort Suite with 0.70 threshold
        System.out.println("==========================================================================");
        System.out.println("              CI/CD AUTOMATED QUALITY GATE AUDIT                          ");
        System.out.println("==========================================================================");
        var result = suite.runSuite(0.70);
        System.out.println("Total Evaluated Cases   : " + result.totalCases());
        System.out.println("Passed Cases (All Triad): " + result.passedCases());
        System.out.println("Failed Cases            : " + result.failedCases());
        System.out.printf("Average Context Rel     : %.2f%n", result.avgContextRelevance());
        System.out.printf("Average Groundedness    : %.2f%n", result.avgGroundedness());
        System.out.printf("Average Answer Rel      : %.2f%n", result.avgAnswerRelevance());
        System.out.printf("Average Composite Score : %.2f%n", result.avgCompositeScore());
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("CI/CD Build Quality Gate: " + (result.ciGatePassed() ? "PASSED (DEPLOYMENT APPROVED)" : "FAILED (DEPLOYMENT BLOCKED)"));
        System.out.println("==========================================================================");
        System.out.println(">>> RAG Triad evaluation and testing verification completed successfully!");
    }
}
