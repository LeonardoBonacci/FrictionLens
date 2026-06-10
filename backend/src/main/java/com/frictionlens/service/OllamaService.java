package com.frictionlens.service;

import com.frictionlens.config.OllamaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

    private final WebClient webClient;
    private final OllamaConfig ollamaConfig;

    public OllamaService(OllamaConfig ollamaConfig, WebClient.Builder webClientBuilder) {
        this.ollamaConfig = ollamaConfig;
        this.webClient = webClientBuilder.baseUrl(ollamaConfig.getBaseUrl()).build();
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

            Map<?, ?> response = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

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

            Map<?, ?> response = webClient.post()
                    .uri("/api/embed")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

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
}
