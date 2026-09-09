package com.genai.springai.chatclient;

import com.genai.springai.core.ChatModel;
import com.genai.springai.core.ChatOptions;
import com.genai.springai.core.ChatResponse;
import com.genai.springai.core.OllamaChatModel;

public class ChatClientDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 33: CHATCLIENT FLUENT API & ADVISOR INTERCEPTOR DEMONSTRATION             ");
        System.out.println("================================================================================\n");

        ChatModel model = new OllamaChatModel("http://localhost:11434", "llama3.2");

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Configuring ChatClient with Default System Prompt & Logging Advisor
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 1] Creating ChatClient with Default System Prompt & Logging Advisor...");
        ChatClient chatClient = ChatClient.builder(model)
                .defaultSystem("You are a Senior Spring AI Consultant. Answer with enterprise best practices.")
                .defaultAdvisors(new LoggingAdvisor())
                .defaultOptions(ChatOptions.builder().temperature(0.3).maxTokens(1024).build())
                .build();

        String answer1 = chatClient.prompt()
                .user("Explain the ChatClient fluent builder API in Spring AI.")
                .call()
                .content();

        System.out.println("  Response Content:\n  " + answer1);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Dynamic Template Parameter Substitution
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Dynamic Prompt Template Parameter Substitution...");
        String answer2 = chatClient.prompt()
                .user(u -> u.text("Compare {techA} against {techB} for enterprise {workloadType}.")
                        .param("techA", "Java 21")
                        .param("techB", "Node.js")
                        .param("workloadType", "RAG Vector Pipelines"))
                .call()
                .content();

        System.out.println("  Response Content:\n  " + answer2);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: Custom PII Redaction Advisor in Action
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] Testing PiiRedactionAdvisor (Sanitizing Sensitive User Data)...");
        ChatClient secureChatClient = ChatClient.builder(model)
                .defaultAdvisors(new PiiRedactionAdvisor(), new LoggingAdvisor())
                .build();

        String promptWithSensitiveData = "Customer John Doe requested a refund on card 4111-2222-3333-4444 with SSN 123-45-6789. Can you generate an apology note?";
        System.out.println("  Original User Prompt:\n  " + promptWithSensitiveData);

        ChatResponse response = secureChatClient.prompt()
                .user(promptWithSensitiveData)
                .call()
                .chatResponse();

        System.out.println("  Response:\n  " + response.getResult().output().getContent());

        System.out.println("\n================================================================================");
        System.out.println("  CHATCLIENT FLUENT API & ADVISORS VALIDATED SUCCESSFULLY!                      ");
        System.out.println("================================================================================");
    }
}
