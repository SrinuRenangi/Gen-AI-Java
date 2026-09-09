package com.javagenai.day09.mini_ioc;

@MyComponent
public class ChatEngine {
    public String askLLM(String prompt) {
        return "[Simulated LLM Response to: '" + prompt + "']";
    }
}
