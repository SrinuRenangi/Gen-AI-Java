package com.javagenai.day12;

import java.util.HashMap;
import java.util.Map;

public class ConditionalEvaluationSimulator {
    private final Map<String, String> properties = new HashMap<>();
    private final Map<Class<?>, Object> beanContext = new HashMap<>();

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public void registerUserBean(Class<?> type, Object instance) {
        beanContext.put(type, instance);
        System.out.println("[USER BEAN REGISTERED]: " + type.getSimpleName() + " provided by developer.");
    }

    // Simulating Spring Boot Auto-Configuration Engine
    public void runAutoConfiguration() {
        System.out.println("\n[CONDITIONS EVALUATION REPORT]");

        // Rule 1: Check @ConditionalOnProperty("spring.ai.openai.api-key")
        String apiKey = properties.get("spring.ai.openai.api-key");
        if (apiKey != null && !apiKey.isBlank()) {
            System.out.println("  Positive Match: OpenAiAutoConfiguration matched on property 'spring.ai.openai.api-key'.");
            if (!beanContext.containsKey(ChatModel.class)) {
                // @ConditionalOnMissingBean matched!
                beanContext.put(ChatModel.class, (ChatModel) prompt -> "[Auto-Configured OpenAI Client] " + prompt);
                System.out.println("  -> Auto-configured OpenAiChatModel bean registered!");
            } else {
                System.out.println("  -> @ConditionalOnMissingBean: Skipped (Developer provided their own ChatModel).");
            }
            return;
        }

        // Rule 2: Fallback to local Ollama if no cloud key configured
        System.out.println("  Negative Match: OpenAiAutoConfiguration rejected (no api-key found).");
        if (!beanContext.containsKey(ChatModel.class)) {
            System.out.println("  Positive Match: FallbackOllamaAutoConfiguration matched on @ConditionalOnMissingBean.");
            beanContext.put(ChatModel.class, (ChatModel) prompt -> "[Auto-Configured Local Ollama Client] " + prompt);
            System.out.println("  -> Auto-configured OllamaChatModel fallback bean registered!");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        return (T) beanContext.get(type);
    }
}
