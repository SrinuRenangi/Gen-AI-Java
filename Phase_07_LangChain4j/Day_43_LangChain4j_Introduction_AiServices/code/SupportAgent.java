package com.genai.langchain4j.aiservices;

/**
 * Declarative AI Service interface for customer support workflows.
 */
@SystemMessage("You are an enterprise technical support specialist for Acme Cloud Solutions.")
public interface SupportAgent {

    @UserMessage("Customer {{customerName}} inquires: {{issueDescription}}")
    String handleCustomerQuery(@V("customerName") String name, @V("issueDescription") String issue);

    @UserMessage
    String directChat(String prompt);

    @UserMessage("Audit this message and return token analytics: {{message}}")
    ModelResponse auditMessage(@V("message") String message);
}
