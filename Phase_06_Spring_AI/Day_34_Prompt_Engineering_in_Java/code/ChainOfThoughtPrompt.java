package com.genai.springai.prompt;

import java.util.List;

/**
 * Builds Chain-of-Thought (CoT) prompts forcing models to reason step-by-step
 * before arriving at a final deterministic conclusion.
 */
public class ChainOfThoughtPrompt {

    public static String build(String problemStatement, List<String> requiredAnalysisSteps) {
        StringBuilder sb = new StringBuilder();
        sb.append("<persona>\n");
        sb.append("You are an analytical reasoning engine. Do NOT guess or skip calculations.\n");
        sb.append("</persona>\n\n");

        sb.append("<problem>\n");
        sb.append(problemStatement.trim()).append("\n");
        sb.append("</problem>\n\n");

        sb.append("<reasoning_framework>\n");
        sb.append("Follow these explicit steps inside <thought> tags before producing your final answer:\n");
        for (int i = 0; i < requiredAnalysisSteps.size(); i++) {
            sb.append("Step ").append(i + 1).append(": ").append(requiredAnalysisSteps.get(i)).append("\n");
        }
        sb.append("</reasoning_framework>\n\n");

        sb.append("<output_format>\n");
        sb.append("Provide your response strictly in the following format:\n");
        sb.append("<thought>\n[Detailed step-by-step reasoning following each numbered step above]\n</thought>\n");
        sb.append("<final_answer>\n[Direct, authoritative conclusion]\n</final_answer>\n");
        sb.append("</output_format>");

        return sb.toString();
    }
}
