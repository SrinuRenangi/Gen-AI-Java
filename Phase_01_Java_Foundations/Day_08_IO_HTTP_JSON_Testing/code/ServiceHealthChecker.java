package com.javagenai.day08;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class ServiceHealthChecker {

    public static boolean isServiceAlive(String url) {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(2))
            .build();

        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
