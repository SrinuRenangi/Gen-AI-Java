package com.genai.springai.core;

import java.util.Map;

/**
 * Simulates Spring AI's Message hierarchy:
 * - SystemMessage (Behavioral instructions)
 * - UserMessage (End-user queries)
 * - AssistantMessage (LLM responses)
 */
public interface Message {

    enum MessageType {
        SYSTEM, USER, ASSISTANT
    }

    MessageType getMessageType();
    String getContent();
    Map<String, Object> getMetadata();

    record SystemMessage(String content, Map<String, Object> metadata) implements Message {
        public SystemMessage(String content) {
            this(content, Map.of());
        }
        @Override
        public MessageType getMessageType() {
            return MessageType.SYSTEM;
        }
        @Override
        public String getContent() {
            return content;
        }
        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    record UserMessage(String content, Map<String, Object> metadata) implements Message {
        public UserMessage(String content) {
            this(content, Map.of());
        }
        @Override
        public MessageType getMessageType() {
            return MessageType.USER;
        }
        @Override
        public String getContent() {
            return content;
        }
        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    record AssistantMessage(String content, Map<String, Object> metadata) implements Message {
        public AssistantMessage(String content) {
            this(content, Map.of());
        }
        @Override
        public MessageType getMessageType() {
            return MessageType.ASSISTANT;
        }
        @Override
        public String getContent() {
            return content;
        }
        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}
