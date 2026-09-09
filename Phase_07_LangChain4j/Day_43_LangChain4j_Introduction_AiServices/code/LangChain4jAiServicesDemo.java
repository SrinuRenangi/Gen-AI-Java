package com.genai.langchain4j.aiservices;

/**
 * Executable demonstration of Day 43:
 * LangChain4j Introduction & Declarative AiServices.
 */
public class LangChain4jAiServicesDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  DAY 43: LANGCHAIN4J AISERVICES DECLARATIVE INTERFACES DEMO     ");
        System.out.println("==================================================================");

        // 1. Initialize the ChatLanguageModel
        ChatLanguageModel model = new SimulatedChatModel();

        // 2. Instantiate Declarative AI Services via Dynamic Proxy
        SupportAgent supportAgent = AiServices.create(SupportAgent.class, model);
        SentimentClassifier classifier = AiServices.create(SentimentClassifier.class, model);

        // 3. Test Templated Prompt with @V Parameter Binding
        System.out.println("\n--- 1. Declarative Prompt with @V Variable Substitution ---");
        String response1 = supportAgent.handleCustomerQuery("Sarah Jenkins", "I would like to process a refund for my cloud storage subscription.");
        System.out.println("Agent Response:\n" + response1);

        // 4. Test Direct Chat Method
        System.out.println("\n--- 2. Direct Chat Invocation ---");
        String response2 = supportAgent.directChat("Hello there, what is your primary support tier?");
        System.out.println("Agent Response:\n" + response2);

        // 5. Test Return Type Conversion to Java Enum
        System.out.println("\n--- 3. Declarative Return-Type Conversion to Enum ---");
        SentimentClassifier.Sentiment review1 = classifier.classify("This new virtual thread feature is stellar and lightning fast!");
        SentimentClassifier.Sentiment review2 = classifier.classify("The database crashed completely and all transactions were lost. Terrible!");
        SentimentClassifier.Sentiment review3 = classifier.classify("The server restarted at 03:00 AM UTC per standard schedule.");

        System.out.println("Review 1 ('stellar, fast'):  Sentiment." + review1);
        System.out.println("Review 2 ('crashed, terrible'): Sentiment." + review2);
        System.out.println("Review 3 ('restarted schedule'): Sentiment." + review3);

        // 6. Test Token Analytics Retrieval
        System.out.println("\n--- 4. Telemetry and Token Accounting ---");
        ModelResponse audit = supportAgent.auditMessage("Can you provide an SLA report for the EMEA region?");
        System.out.println("Output Content: " + audit.content());
        System.out.printf("Tokens Consumed: [Input: %d, Output: %d, Total: %d]\n",
            audit.tokenUsage().inputTokens(), audit.tokenUsage().outputTokens(), audit.tokenUsage().totalTokens());

        System.out.println("\n==================================================================");
        System.out.println("  LANGCHAIN4J AISERVICES DEMO COMPLETED SUCCESSFULLY             ");
        System.out.println("==================================================================");
    }
}
