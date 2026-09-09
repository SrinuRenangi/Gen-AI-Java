package com.javagenai.day03;

public class AIAssistantService {
    private ChatModel chatModel;

    public AIAssistantService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String answerUserQuery(String query) {
        return this.chatModel.call(query);
    }
}
