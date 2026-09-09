package com.javagenai.day11;

public class AIModelRouter {
    private final ChatModel fastModel;
    private final ChatModel deepModel;

    public AIModelRouter(ChatModel fastModel, ChatModel deepModel) {
        this.fastModel = fastModel;
        this.deepModel = deepModel;
    }

    public String routeAndExecute(String prompt) {
        if (prompt.length() < 60) {
            System.out.println("[ROUTER]: Routing short prompt to FastModel...");
            return fastModel.call(prompt);
        } else {
            System.out.println("[ROUTER]: Routing complex prompt to DeepModel...");
            return deepModel.call(prompt);
        }
    }
}
