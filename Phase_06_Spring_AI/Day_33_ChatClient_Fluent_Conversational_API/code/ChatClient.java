package com.genai.springai.chatclient;

import com.genai.springai.core.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Advanced ChatClient implementing Spring AI's modern fluent API:
 * - Advisor Interceptor Chain (before/after hooks)
 * - Dynamic Template Parameter Substitution
 * - Entity Object Mapping (.entity(Class<T>))
 */
public class ChatClient {

    private final ChatModel chatModel;
    private final String defaultSystemMessage;
    private final List<Advisor> defaultAdvisors;
    private final ChatOptions defaultOptions;

    private ChatClient(Builder builder) {
        this.chatModel = builder.chatModel;
        this.defaultSystemMessage = builder.defaultSystem;
        this.defaultAdvisors = new ArrayList<>(builder.defaultAdvisors);
        this.defaultOptions = builder.defaultOptions != null ? builder.defaultOptions : ChatOptions.defaults();
    }

    public static Builder builder(ChatModel chatModel) {
        return new Builder(chatModel);
    }

    public ChatClientRequestSpec prompt() {
        ChatClientRequestSpec spec = new ChatClientRequestSpec(this);
        if (defaultSystemMessage != null && !defaultSystemMessage.isBlank()) {
            spec.system(defaultSystemMessage);
        }
        spec.advisors(defaultAdvisors);
        spec.options(defaultOptions);
        return spec;
    }

    public static class Builder {
        private final ChatModel chatModel;
        private String defaultSystem;
        private final List<Advisor> defaultAdvisors = new ArrayList<>();
        private ChatOptions defaultOptions;

        public Builder(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        public Builder defaultSystem(String defaultSystem) {
            this.defaultSystem = defaultSystem;
            return this;
        }

        public Builder defaultAdvisors(Advisor... advisors) {
            this.defaultAdvisors.addAll(Arrays.asList(advisors));
            return this;
        }

        public Builder defaultOptions(ChatOptions defaultOptions) {
            this.defaultOptions = defaultOptions;
            return this;
        }

        public ChatClient build() {
            return new ChatClient(this);
        }
    }

    public static class ChatClientRequestSpec {
        private final ChatClient client;
        private final List<Message> messages = new ArrayList<>();
        private final List<Advisor> advisors = new ArrayList<>();
        private ChatOptions options;

        public ChatClientRequestSpec(ChatClient client) {
            this.client = client;
        }

        public ChatClientRequestSpec system(String systemText) {
            messages.add(new Message.SystemMessage(systemText));
            return this;
        }

        public ChatClientRequestSpec system(Consumer<PromptUserSpec> consumer) {
            PromptUserSpec spec = new PromptUserSpec();
            consumer.accept(spec);
            messages.add(new Message.SystemMessage(spec.render()));
            return this;
        }

        public ChatClientRequestSpec user(String userText) {
            messages.add(new Message.UserMessage(userText));
            return this;
        }

        public ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer) {
            PromptUserSpec spec = new PromptUserSpec();
            consumer.accept(spec);
            messages.add(new Message.UserMessage(spec.render()));
            return this;
        }

        public ChatClientRequestSpec options(ChatOptions options) {
            this.options = options;
            return this;
        }

        public ChatClientRequestSpec advisors(List<Advisor> additionalAdvisors) {
            this.advisors.addAll(additionalAdvisors);
            return this;
        }

        public ChatClientRequestSpec advisors(Advisor... additionalAdvisors) {
            this.advisors.addAll(Arrays.asList(additionalAdvisors));
            return this;
        }

        public CallResponseSpec call() {
            Prompt prompt = new Prompt(messages, options);

            // Execute Advisors: before() hooks
            for (Advisor advisor : advisors) {
                prompt = advisor.before(prompt);
            }

            // Execute low-level ChatModel SPI
            ChatResponse response = client.chatModel.call(prompt);

            // Execute Advisors: after() hooks (in reverse order)
            for (int i = advisors.size() - 1; i >= 0; i--) {
                response = advisors.get(i).after(response);
            }

            return new CallResponseSpec(response);
        }
    }

    public static class PromptUserSpec {
        private String templateText = "";
        private final Map<String, Object> params = new HashMap<>();

        public PromptUserSpec text(String text) {
            this.templateText = text;
            return this;
        }

        public PromptUserSpec param(String key, Object value) {
            this.params.put(key, value);
            return this;
        }

        public PromptUserSpec params(Map<String, Object> map) {
            this.params.putAll(map);
            return this;
        }

        public String render() {
            return new PromptTemplate(templateText).render(params);
        }
    }

    public record CallResponseSpec(ChatResponse response) {
        public String content() {
            return response.getResult().output().getContent();
        }

        public ChatResponse chatResponse() {
            return response;
        }
    }
}
