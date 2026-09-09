package com.javagenai.day12;

public class AutoConfigDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 12: SPRING BOOT AUTO-CONFIGURATION DEMO    ");
        System.out.println("==================================================");

        // SCENARIO 1: No properties, no custom beans -> Falls back to local Ollama
        System.out.println("--- SCENARIO 1: Fresh project, zero configuration ---");
        ConditionalEvaluationSimulator context1 = new ConditionalEvaluationSimulator();
        context1.runAutoConfiguration();
        ChatModel model1 = context1.getBean(ChatModel.class);
        System.out.println("Result: " + model1.call("Hello World"));
        System.out.println();

        // SCENARIO 2: OpenAI API key configured in application.yml -> Auto-configures OpenAI
        System.out.println("--- SCENARIO 2: Developer adds API key to application.yml ---");
        ConditionalEvaluationSimulator context2 = new ConditionalEvaluationSimulator();
        context2.setProperty("spring.ai.openai.api-key", "sk-live-12345");
        context2.runAutoConfiguration();
        ChatModel model2 = context2.getBean(ChatModel.class);
        System.out.println("Result: " + model2.call("Hello World"));
        System.out.println();

        // SCENARIO 3: Developer provides their own custom @Bean -> Overrides auto-config!
        System.out.println("--- SCENARIO 3: Developer defines custom @Bean ChatModel ---");
        ConditionalEvaluationSimulator context3 = new ConditionalEvaluationSimulator();
        context3.setProperty("spring.ai.openai.api-key", "sk-live-12345");
        context3.registerUserBean(ChatModel.class, (ChatModel) prompt -> "[CUSTOM Anthropic Claude-3.5 Bean]: " + prompt);
        context3.runAutoConfiguration();
        ChatModel model3 = context3.getBean(ChatModel.class);
        System.out.println("Result: " + model3.call("Hello World"));
        System.out.println("==================================================");
    }
}
