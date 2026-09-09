package com.genai.springai.streaming;

import java.util.function.Consumer;

/**
 * Interface representing a reactive streaming LLM client.
 * Dispatches tokens as they are produced by the neural network.
 */
public interface StreamingChatModel {

    void stream(
            String prompt,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError
    );
}
