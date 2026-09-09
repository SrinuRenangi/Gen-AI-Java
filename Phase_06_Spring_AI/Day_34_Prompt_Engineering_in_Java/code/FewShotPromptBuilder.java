package com.genai.springai.prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds structured Few-Shot prompts containing instructions, exemplar pairs, and the target input.
 */
public class FewShotPromptBuilder {

    private String instruction;
    private final List<FewShotExample> examples = new ArrayList<>();
    private String examplePrefix = "Example Input:";
    private String exampleSuffix = "Example Output:";
    private String inputPrefix = "Input:";
    private String outputPrefix = "Output:";

    public FewShotPromptBuilder instruction(String instruction) {
        this.instruction = instruction;
        return this;
    }

    public FewShotPromptBuilder addExample(String input, String output) {
        this.examples.add(new FewShotExample(input, output));
        return this;
    }

    public FewShotPromptBuilder prefixes(String examplePrefix, String exampleSuffix, String inputPrefix, String outputPrefix) {
        this.examplePrefix = examplePrefix;
        this.exampleSuffix = exampleSuffix;
        this.inputPrefix = inputPrefix;
        this.outputPrefix = outputPrefix;
        return this;
    }

    public String build(String targetInput) {
        StringBuilder sb = new StringBuilder();

        if (instruction != null && !instruction.isBlank()) {
            sb.append("<instructions>\n").append(instruction.trim()).append("\n</instructions>\n\n");
        }

        if (!examples.isEmpty()) {
            sb.append("<examples>\n");
            for (int i = 0; i < examples.size(); i++) {
                FewShotExample ex = examples.get(i);
                sb.append("--- Example ").append(i + 1).append(" ---\n");
                sb.append(examplePrefix).append(" ").append(ex.input()).append("\n");
                sb.append(exampleSuffix).append(" ").append(ex.output()).append("\n\n");
            }
            sb.append("</examples>\n\n");
        }

        sb.append("<query>\n");
        sb.append(inputPrefix).append(" ").append(targetInput).append("\n");
        sb.append(outputPrefix);
        sb.append("\n</query>");

        return sb.toString();
    }
}
