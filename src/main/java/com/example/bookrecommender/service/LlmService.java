package com.example.bookrecommender.service;

import com.example.bookrecommender.model.BookDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class LlmService {
    private final boolean enabled;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public LlmService(
            @Value("${llm.enabled:false}") boolean enabled,
            @Value("${llm.api-url:http://127.0.0.1:1234/v1/chat/completions}") String apiUrl,
            @Value("${llm.api-key:}") String apiKey,
            @Value("${llm.model:local-model}") String model
    ) {
        this.enabled = enabled;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String answerWithContext(String question, List<BookDocument> documents) {
        if (!enabled) {
            throw new IllegalStateException("LLM service is disabled.");
        }

        if (documents == null || documents.isEmpty()) {
            return "I could not find relevant information in the RDF book database for this question.";
        }

        Instant start = Instant.now();

        try {
            String prompt = buildPrompt(question, documents);
            String requestBody = buildRequestBody(prompt);

            System.out.println("[LLM] Sending request to LM Studio");
            System.out.println("[LLM] API URL: " + apiUrl);
            System.out.println("[LLM] Model: " + model);
            System.out.println("[LLM] Question: " + question);
            System.out.println("[LLM] Retrieved documents: " + documents.size());

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            long elapsedMs = Duration.between(start, Instant.now()).toMillis();

            System.out.println("[LLM] Response status: " + response.statusCode());
            System.out.println("[LLM] Response time: " + elapsedMs + " ms");
            System.out.println("[LLM] Raw response: " + response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "LLM API returned status "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            return extractAnswer(response.body());
        } catch (Exception e) {
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();

            System.err.println("[LLM] Failed after " + elapsedMs + " ms");
            e.printStackTrace();

            throw new IllegalStateException("Could not get answer from LLM service.", e);
        }
    }

    private String buildPrompt(String question, List<BookDocument> documents) {
        StringBuilder context = new StringBuilder();

        for (BookDocument document : documents) {
            context.append("Title: ").append(nullSafe(document.getTitle())).append("\n");
            context.append("Author: ").append(nullSafe(document.getAuthor())).append("\n");
            context.append("Themes: ").append(String.join(", ", document.getThemes())).append("\n");
            context.append("Reading level: ").append(nullSafe(document.getReadingLevel())).append("\n");

            if (!nullSafe(document.getDescription()).isBlank()) {
                context.append("Description: ").append(nullSafe(document.getDescription())).append("\n");
            }

            if (!document.getRecommendedUsers().isEmpty()) {
                context.append("Recommended users: ")
                        .append(String.join(", ", document.getRecommendedUsers()))
                        .append("\n");
            }

            context.append("---\n");
        }

        return """
                You are a book recommendation assistant.

                Answer using only the RDF book context below.
                Do not use outside knowledge.
                Keep the answer short.

                RDF context:
                %s

                User question:
                %s
                """.formatted(context.toString(), nullSafe(question));
    }

    private String buildRequestBody(String prompt) throws Exception {
        String escapedModel = objectMapper.writeValueAsString(model);
        String escapedPrompt = objectMapper.writeValueAsString(prompt);

        return """
                {
                  "model": %s,
                  "messages": [
                    {
                      "role": "system",
                      "content": "Answer only from the provided RDF context. Do not use outside knowledge."
                    },
                    {
                      "role": "user",
                      "content": %s
                    }
                  ],
                  "temperature": 0.1,
                  "max_tokens": 120,
                  "stream": false
                }
                """.formatted(escapedModel, escapedPrompt);
    }

    private String extractAnswer(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root
                .path("choices")
                .path(0)
                .path("message")
                .path("content");

        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("LLM response did not contain choices[0].message.content.");
        }

        return content.asText().trim();
    }

    private String nullSafe(String text) {
        if (text == null) {
            return "";
        }

        return text;
    }
}