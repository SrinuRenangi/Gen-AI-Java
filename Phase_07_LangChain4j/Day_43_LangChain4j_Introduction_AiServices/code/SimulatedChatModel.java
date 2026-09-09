package com.genai.langchain4j.aiservices;

import java.util.List;

/**
 * Deterministic local simulation of a frontier ChatLanguageModel.
 */
public class SimulatedChatModel implements ChatLanguageModel {

    @Override
    public ModelResponse generate(List<ChatMessage> messages) {
        String lastUserPrompt = "";
        String systemPrompt = "";

        for (ChatMessage msg : messages) {
            if (msg.role() == ChatMessage.Role.SYSTEM) systemPrompt = msg.text();
            if (msg.role() == ChatMessage.Role.USER) lastUserPrompt = msg.text();
        }

        String lower = lastUserPrompt.toLowerCase();
        String response;

        if (systemPrompt.toLowerCase().contains("sentiment")) {
            if (lower.contains("great") || lower.contains("love") || lower.contains("happy") || lower.contains("fast") || lower.contains("stellar")) {
                response = "POSITIVE";
            } else if (lower.contains("terrible") || lower.contains("broken") || lower.contains("bad") || lower.contains("crash") || lower.contains("unacceptable")) {
                response = "NEGATIVE";
            } else {
                response = "NEUTRAL";
            }
        } else if (lower.contains("refund") || lower.contains("return")) {
            response = "I understand you wish to request a refund. Under Acme Cloud Solutions policy, service refund requests are eligible within 30 days of billing cycle generation.";
        } else if (lower.contains("hello") || lower.contains("hi")) {
            response = "Hello! I am your Acme Enterprise Support Assistant. How may I assist your engineering team today?";
        } else {
            response = "Thank you for reaching out to Acme Enterprise Support. We have logged your inquiry: '" + lastUserPrompt + "' under Priority SLA Tier 1.";
        }

        int inTok = Math.max(8, (systemPrompt.length() + lastUserPrompt.length()) / 4);
        int outTok = Math.max(4, response.length() / 4);

        return new ModelResponse(response, new TokenUsage(inTok, outTok, inTok + outTok));
    }
}
