/**
 * Day 01 - Exercise 2: Command-Line Token Cost Calculator
 * Calculates LLM inference cost based on token counts passed via CLI args.
 */
public class TokenCostCalculator {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("==================================================");
            System.out.println("Usage: java TokenCostCalculator <model> <inputTokens> <outputTokens>");
            System.out.println("Example: java TokenCostCalculator gpt-4o-mini 2500 800");
            System.out.println("==================================================");
            return;
        }

        String model = args[0];
        long inputTokens = Long.parseLong(args[1]);
        long outputTokens = Long.parseLong(args[2]);

        // Pricing per million tokens (standard baseline)
        double inputPricePerMillion = 0.150;
        double outputPricePerMillion = 0.600;

        double inputCost = (inputTokens / 1_000_000.0) * inputPricePerMillion;
        double outputCost = (outputTokens / 1_000_000.0) * outputPricePerMillion;
        double totalCost = inputCost + outputCost;

        System.out.println("==================================================");
        System.out.println("         LLM API CALL COST BREAKDOWN              ");
        System.out.println("==================================================");
        System.out.println("Model Chosen : " + model);
        System.out.printf("Input Tokens : %,d tokens ($%.6f)%n", inputTokens, inputCost);
        System.out.printf("Output Tokens: %,d tokens ($%.6f)%n", outputTokens, outputCost);
        System.out.println("--------------------------------------------------");
        System.out.printf("Total Cost   : $%.6f USD%n", totalCost);
        System.out.println("==================================================");
    }
}
