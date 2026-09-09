package com.genai.springai.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simulates Spring AI's Prompt object encapsulating messages and model options.
 */
public record Prompt(
        List<Message> messages,
        ChatOptions options
) {
    public Prompt(List<Message> messages) {
        this(messages, ChatOptions.defaults());
    }

    public Prompt(String singleUserPrompt) {
        this(List.of(new Message.UserMessage(singleUserPrompt)), ChatOptions.defaults());
    }

    public Prompt {
        messages = Collections.unmodifiableList(new ArrayList<>(messages));
    }
}
