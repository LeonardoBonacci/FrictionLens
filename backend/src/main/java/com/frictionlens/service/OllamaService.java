package com.frictionlens.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frictionlens.config.OllamaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);

    private static final String SANITIZATION_PROMPT = """
            You are a privacy filter. Your ONLY job is to rewrite the following text so that \
            all personal identifiers are removed. Replace:
            - People's names with generic roles (e.g., "a colleague", "my manager")
            - Email addresses with "[email removed]"
            - Phone numbers with "[phone removed]"
            - Any other personally identifiable information with a generic placeholder
            
            Return ONLY the rewritten text. Do not add any commentary, explanation, or preamble. \
            Preserve the original meaning and tone as closely as possible.
            
            Text to sanitize:
            """;

    private static final String QUERY_INTERPRETATION_PROMPT = """
            You are a query interpreter for a workplace friction reporting system. \
            Given a user's natural language question about workplace friction or blockers, \
            extract any structured filters that are explicitly or implicitly mentioned.
            
            Return a JSON object with these optional fields (include only if clearly mentioned):
            - "jobTitle": a specific job role/title (e.g., "Software Engineer", "Designer")
            - "team": a specific team name (e.g., "Platform", "UX")
            - "category": a specific category (e.g., "Tooling", "Process", "Communication")
            - "severity": a severity level (one of: LOW, MEDIUM, HIGH, CRITICAL)
            
            Return ONLY valid JSON. If no filters can be extracted, return {}.
            """;

    private static final String SUMMARIZATION_PROMPT = """
            You are an analyst summarizing workplace friction reports. Given a user's question \
            and a set of relevant friction reports, provide a concise, actionable summary.
            
            Rules:
            - Base your answer ONLY on the provided reports. Do not invent information.
            - If the reports don't contain enough information to answer the question, say so.
            - Highlight common themes and patterns across reports.
            - Mention specific details from the reports to ground your summary.
            - Keep the summary concise (2-4 paragraphs).
            """;

    private final RestClient restClient;
    private final OllamaConfig ollamaConfig;
    private final ObjectMapper objectMapper;

    public OllamaService(OllamaConfig ollamaConfig, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.ollamaConfig = ollamaConfig;
        this.objectMapper = objectMapper;

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(ollamaConfig.getConnectTimeoutMs());
        requestFactory.setReadTimeout(ollamaConfig.getReadTimeoutMs());

        this.restClient = restClientBuilder
                .baseUrl(ollamaConfig.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Checks whether the Ollama instance is reachable and responsive.
     */
    public boolean isAvailable() {
        try {
            restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }

    public String sanitizeText(String text) {
        return executeWithRetry("sanitization", () -> {
            Map<String, Object> request = Map.of(
                    "model", ollamaConfig.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", SANITIZATION_PROMPT),
                            Map.of("role", "user", "content", text)
                    ),
                    "stream", false
            );

            Map<?, ?> response = restClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("message") instanceof Map<?, ?> message) {
                String content = (String) message.get("content");
                if (content != null && !content.isBlank()) {
                    return content.strip();
                }
            }
            return null;
        }, text, "Ollama sanitization returned empty response, using original text");
    }

    public float[] generateEmbedding(String text) {
        return executeWithRetry("embedding", () -> {
            Map<String, Object> request = Map.of(
                    "model", ollamaConfig.getModel(),
                    "input", text
            );

            Map<?, ?> response = restClient.post()
                    .uri("/api/embed")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("embeddings") instanceof List<?> embeddings
                    && !embeddings.isEmpty() && embeddings.getFirst() instanceof List<?> vector) {
                float[] result = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    result[i] = ((Number) vector.get(i)).floatValue();
                }
                return result;
            }
            return null;
        }, null, "Ollama embedding returned empty response");
    }

    public Map<String, String> interpretQuery(String question) {
        return executeWithRetry("query interpretation", () -> {
            Map<String, Object> request = Map.of(
                    "model", ollamaConfig.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", QUERY_INTERPRETATION_PROMPT),
                            Map.of("role", "user", "content", question)
                    ),
                    "stream", false
            );

            Map<?, ?> response = restClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("message") instanceof Map<?, ?> message) {
                String content = (String) message.get("content");
                if (content != null && !content.isBlank()) {
                    String json = content.strip();
                    if (json.startsWith("```")) {
                        json = json.replaceAll("^```\\w*\\n?", "").replaceAll("\\n?```$", "").strip();
                    }
                    return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
                }
            }
            return null;
        }, Collections.emptyMap(), "Ollama query interpretation returned empty response");
    }

    public String summarizeReports(String question, List<String> reportTexts) {
        String fallback = "Unable to generate summary. Please review the supporting reports below.";
        return executeWithRetry("summarization", () -> {
            StringBuilder reportsBlock = new StringBuilder();
            for (int i = 0; i < reportTexts.size(); i++) {
                reportsBlock.append(String.format("Report %d: %s%n", i + 1, reportTexts.get(i)));
            }

            String userMessage = String.format("Question: %s%n%nRelevant reports:%n%s", question, reportsBlock);

            Map<String, Object> request = Map.of(
                    "model", ollamaConfig.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", SUMMARIZATION_PROMPT),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "stream", false
            );

            Map<?, ?> response = restClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("message") instanceof Map<?, ?> message) {
                String content = (String) message.get("content");
                if (content != null && !content.isBlank()) {
                    return content.strip();
                }
            }
            return null;
        }, fallback, "Ollama summarization returned empty response");
    }

    /**
     * Executes an Ollama operation with retry logic for transient failures.
     * Retries on connection/timeout errors and 503 Service Unavailable responses.
     */
    private <T> T executeWithRetry(String operationName, OllamaOperation<T> operation,
                                   T fallback, String emptyResponseMessage) {
        int maxRetries = ollamaConfig.getMaxRetries();
        long retryDelay = ollamaConfig.getRetryDelayMs();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                T result = operation.execute();
                if (result != null) {
                    return result;
                }
                log.warn(emptyResponseMessage);
                return fallback;
            } catch (ResourceAccessException e) {
                if (attempt < maxRetries) {
                    log.warn("Ollama {} attempt {}/{} failed (connection error), retrying in {}ms: {}",
                            operationName, attempt + 1, maxRetries + 1, retryDelay, e.getMessage());
                    retryDelay = sleepAndBackoff(operationName, retryDelay, fallback);
                    if (retryDelay < 0) return fallback;
                } else {
                    log.error("Ollama {} failed after {} attempts (service unavailable): {}",
                            operationName, maxRetries + 1, e.getMessage());
                    return fallback;
                }
            } catch (HttpServerErrorException e) {
                if (e.getStatusCode().value() == 503 && attempt < maxRetries) {
                    log.warn("Ollama {} attempt {}/{} failed (503 Service Unavailable), retrying in {}ms",
                            operationName, attempt + 1, maxRetries + 1, retryDelay);
                    retryDelay = sleepAndBackoff(operationName, retryDelay, fallback);
                    if (retryDelay < 0) return fallback;
                } else {
                    log.error("Ollama {} failed (HTTP {}): {}",
                            operationName, e.getStatusCode().value(), e.getMessage());
                    return fallback;
                }
            } catch (Exception e) {
                log.error("Ollama {} failed: {}", operationName, e.getMessage());
                return fallback;
            }
        }
        return fallback;
    }

    private <T> long sleepAndBackoff(String operationName, long retryDelay, T fallback) {
        try {
            Thread.sleep(retryDelay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Retry interrupted for Ollama {}", operationName);
            return -1;
        }
        return retryDelay * 2;
    }

    @FunctionalInterface
    private interface OllamaOperation<T> {
        T execute() throws Exception;
    }
}
