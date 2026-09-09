package com.javagenai.day03;

public interface ChatModel {
    String call(String prompt);

    default int estimateTokens(String text) {
        return (text == null) ? 0 : (int) Math.ceil(text.length() / 4.0);
    }
}
