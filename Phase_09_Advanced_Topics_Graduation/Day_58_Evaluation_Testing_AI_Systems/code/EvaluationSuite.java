package com.genai.enterprise.evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise test suite executing cohort evaluations and CI/CD quality gate enforcement.
 */
public class EvaluationSuite {

    public record SuiteResult(
            int totalCases,
            int passedCases,
            int failedCases,
            double avgContextRelevance,
            double avgGroundedness,
            double avgAnswerRelevance,
            double avgCompositeScore,
            boolean ciGatePassed
    ) {}

    private final List<RagTestCase> testCases = new ArrayList<>();
    private final LlmAsAJudgeEvaluator evaluator = new LlmAsAJudgeEvaluator();

    public void addTestCase(RagTestCase testCase) {
        testCases.add(testCase);
    }

    public SuiteResult runSuite(double qualityGateThreshold) {
        double totalCtx = 0.0;
        double totalGround = 0.0;
        double totalAns = 0.0;
        double totalComp = 0.0;
        int passed = 0;

        for (RagTestCase tc : testCases) {
            RagTriadMetrics m = evaluator.evaluate(tc);
            totalCtx += m.contextRelevance();
            totalGround += m.groundedness();
            totalAns += m.answerRelevance();
            totalComp += m.getCompositeScore();

            if (m.isPassing(qualityGateThreshold)) {
                passed++;
            }
        }

        int count = testCases.isEmpty() ? 1 : testCases.size();
        double avgCtx = totalCtx / count;
        double avgGround = totalGround / count;
        double avgAns = totalAns / count;
        double avgComp = totalComp / count;

        boolean gatePassed = (avgGround >= qualityGateThreshold) && (avgAns >= qualityGateThreshold);

        return new SuiteResult(
                testCases.size(), passed, testCases.size() - passed,
                avgCtx, avgGround, avgAns, avgComp, gatePassed
        );
    }

    public LlmAsAJudgeEvaluator getEvaluator() { return evaluator; }
}
