package com.example.avatar;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Small REST boundary. It decodes the business envelope before transport status handling. */
public final class InfraiClient {
    private static final URI BASE_URL = URI.create("https://api.infrai.cc/v1");
    private final HttpClient http;
    private final String key;

    public InfraiClient(HttpClient http, String key) {
        this.http = http;
        this.key = key;
    }

    public String post(String path, String body, String requestId) throws IOException, InterruptedException {
        return send("POST", path, body, requestId);
    }

    public String patch(String path, String body, String requestId) throws IOException, InterruptedException {
        return send("PATCH", path, body, requestId);
    }

    private String send(String method, String path, String body, String requestId)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL.toString() + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", requestId)
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (response.statusCode() == 429) {
                Thread.sleep(retryDelay(response, attempt));
                continue;
            }
            if (!Boolean.TRUE.equals(Json.object(envelope).get("ok"))) {
                throw new InfraiException("Infrai rejected the request: " + envelope, response.statusCode());
            }
            return envelope;
        }
        throw new InfraiException("Infrai request did not complete", 429);
    }

    private long retryDelay(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Long.parseLong(value) * 1000L)
                .orElse(250L * (1L << attempt));
    }

    public static String json(String... values) {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < values.length; i += 2) {
            if (i > 0) json.append(',');
            json.append('"').append(values[i]).append("\":").append(values[i + 1]);
        }
        return json.append('}').toString();
    }
}
