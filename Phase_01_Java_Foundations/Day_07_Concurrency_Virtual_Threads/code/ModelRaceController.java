package com.javagenai.day07;

import java.util.concurrent.CompletableFuture;

public class ModelRaceController {

    public static CompletableFuture<String> queryModel(String modelName, int delayMs, String response) {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
            return String.format("[%s]: %s (latency: %dms)", modelName, response, delayMs);
        });
    }

    public static String getFastestResponse(String prompt) {
        CompletableFuture<String> fastModel = queryModel("Llama-3.2-1B", 100, "Quick local summary");
        CompletableFuture<String> slowModel = queryModel("GPT-4o-Heavy", 400, "In-depth cloud analysis");

        Object winner = CompletableFuture.anyOf(fastModel, slowModel).join();
        return (String) winner;
    }
}
