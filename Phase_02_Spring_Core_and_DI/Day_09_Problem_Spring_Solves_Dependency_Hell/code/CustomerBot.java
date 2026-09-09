package com.javagenai.day09.mini_ioc;

@MyComponent
public class CustomerBot {

    @MyInject
    private ChatEngine chatEngine;

    public String answer(String question) {
        return chatEngine.askLLM(question);
    }
}
