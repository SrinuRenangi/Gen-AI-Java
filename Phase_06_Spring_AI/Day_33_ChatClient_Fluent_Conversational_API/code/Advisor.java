package com.genai.springai.chatclient;

import com.genai.springai.core.ChatResponse;
import com.genai.springai.core.Prompt;

/**
 * Simulates Spring AI's RequestResponseAdvisor interface.
 * Intercepts Prompt before model execution, and intercepts ChatResponse after execution.
 */
public interface Advisor {

    String getName();

    Prompt before(Prompt prompt);

    ChatResponse after(ChatResponse response);
}
