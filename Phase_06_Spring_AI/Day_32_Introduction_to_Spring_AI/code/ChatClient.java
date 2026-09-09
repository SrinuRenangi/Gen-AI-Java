package com.genai.springai.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates Spring AI's modern ChatClient fluent conversational API.
 * Provides a builder and prompt specification chain:
 * chatClient.prompt().system("...").user("...").call().content();
 */
public class ChatClient {

    private final ChatModel chatModel;
    private final String defaultSystemMessage;

    private ChatClient(ChatModel chatModel, String defaultSystemMessage) {
        this.chatModel = chatModel;
        this.defaultSystemMessage = defaultSystemMessage;
    }

    public static Builder builder(ChatModel chatModel) {
        return new Builder(chatModel);
    }

    public ChatClientRequestSpec prompt() {
        ChatClientRequestSpec spec = new ChatClientRequestSpec(chatModel);
        if (defaultSystemMessage != null && !defaultSystemMessage.isBlank()) {
            spec.system(defaultSystemMessage);
        }
        return spec;
    }

    public static class Builder {
        private final ChatModel chatModel;
        private String defaultSystem;

        public Builder(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        public Builder defaultSystem(String defaultSystem) {
            this.defaultSystem = defaultSystem;
            return this;
        }

        public ChatClient build() {
            return new ChatClient(chatModel, defaultSystem);
        }
    }

    public static class ChatClientRequestSpec {
        private final ChatModel chatModel;
        private final List<Message> messages = new ArrayList<>();
        private ChatOptions options = ChatOptions.defaults();

        public ChatClientRequestSpec(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        public ChatClientRequestSpec system(String systemText) {
            messages.add(new Message.SystemMessage(systemText));
            return this;
        }

        public ChatClientRequestSpec user(String userText) {
            messages.add(new Message.UserMessage(userText));
            return this;
        }

        public ChatClientRequestSpec options(ChatOptions options) {
            this.options = options;
            return this;
        }

        public CallResponseSpec call() {
            Prompt prompt = new Prompt(messages, options);
            ChatResponse response = chatModel.call(prompt);
            return new CallResponseSpec(response);
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
