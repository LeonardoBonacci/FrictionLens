package com.frictionlens.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frictionlens.config.OllamaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
        this.restClient = restClientBuilder.baseUrl(ollamaConfig.getBaseUrl()).build();
        this.objectMapper = objectMapper;
    }

    public String sanitizeText(String text) {
        try {
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

            log.warn("Ollama sanitization returned empty response, using original text");
            return text;
        } catch (Exception e) {
            log.error("Ollama sanitization failed, using original text: {}", e.getMessage());
            return text;
        }
    }

    public float[] generateEmbedding(String text) {
        try {
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

            log.warn("Ollama embedding returned empty response");
            return null;
        } catch (Exception e) {
            log.error("Ollama embedding generation failed: {}", e.getMessage());
            return null;
        }
    }

    public Map<String, String> interpretQuery(String question) {
        try {
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
                    // Strip markdown code fences if present
                    if (json.startsWith("```")) {
                        json = json.replaceAll("^```\\w*\\n?", "").replaceAll("\\n?```$", "").strip();
                    }
                    return objectMapper.readValue(json, new TypeReference<>() {});
                }
            }

            log.warn("Ollama query interpretation returned empty response");
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Ollama query interpretation failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public String summarizeReports(String question, List<String> reportTexts) {
        try {
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

            log.warn("Ollama summarization returned empty response");
            return "Unable to generate summary. Please review the supporting reports below.";
        } catch (Exception e) {
            log.error("Ollama summarization failed: {}", e.getMessage());
            return "Unable to generate summary. Please review the supporting reports below.";
        }
    }
}
