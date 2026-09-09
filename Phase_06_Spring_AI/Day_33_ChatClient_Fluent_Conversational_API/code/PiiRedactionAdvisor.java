package com.genai.springai.chatclient;

import com.genai.springai.core.ChatResponse;
import com.genai.springai.core.Message;
import com.genai.springai.core.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Custom Spring AI Advisor that sanitizes sensitive Personally Identifiable Information (PII)
 * such as Credit Card numbers and Social Security Numbers before the prompt leaves the JVM!
 */
public class PiiRedactionAdvisor implements Advisor {

    private static final Pattern CREDIT_CARD_PATTERN = 
            Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b");
    
    private static final Pattern SSN_PATTERN = 
            Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    @Override
    public String getName() {
        return "PiiRedactionAdvisor";
    }

    @Override
    public Prompt before(Prompt prompt) {
        List<Message> sanitized = new ArrayList<>();
        boolean redactedAny = false;

        for (Message msg : prompt.messages()) {
            String content = msg.getContent();
            String redacted = CREDIT_CARD_PATTERN.matcher(content).replaceAll("[REDACTED_CARD]");
            redacted = SSN_PATTERN.matcher(redacted).replaceAll("[REDACTED_SSN]");

            if (!redacted.equals(content)) {
                redactedAny = true;
            }

            if (msg.getMessageType() == Message.MessageType.SYSTEM) {
                sanitized.add(new Message.SystemMessage(redacted, msg.getMetadata()));
            } else if (msg.getMessageType() == Message.MessageType.USER) {
                sanitized.add(new Message.UserMessage(redacted, msg.getMetadata()));
            } else {
                sanitized.add(new Message.AssistantMessage(redacted, msg.getMetadata()));
            }
        }

        if (redactedAny) {
            System.out.println("  [ADVISOR: PII_GUARD] 🛡️ Sensitive PII detected and redacted before reaching LLM provider!");
        }

        return new Prompt(sanitized, prompt.options());
    }

    @Override
    public ChatResponse after(ChatResponse response) {
        return response;
    }
}
