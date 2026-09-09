package com.genai.springai.chatclient;

import java.util.Map;

/**
 * Simulates Spring AI's PromptTemplate for dynamic variable substitution.
 * Replaces placeholders like {topic} and {style} with runtime parameters.
 */
public class PromptTemplate {

    private final String template;

    public PromptTemplate(String template) {
        this.template = template;
    }

    public String render(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return result;
    }

    public String getTemplate() {
        return template;
    }
}
