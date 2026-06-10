package com.frictionlens.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frictionlens.config.OllamaConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaServiceTest {

    private MockWebServer mockServer;
    private OllamaService ollamaService;

    @BeforeEach
    void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();

        OllamaConfig config = new OllamaConfig();
        config.setBaseUrl(mockServer.url("/").toString());
        config.setModel("llama3.1:latest");

        ollamaService = new OllamaService(config, RestClient.builder(), new ObjectMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        mockServer.shutdown();
    }

    @Test
    void sanitizeText_returnsCleanedText() {
        String responseBody = """
                {
                    "message": {
                        "role": "assistant",
                        "content": "A colleague caused issues with the CI pipeline"
                    }
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        String result = ollamaService.sanitizeText("John Smith caused issues with the CI pipeline");

        assertThat(result).isEqualTo("A colleague caused issues with the CI pipeline");
    }

    @Test
    void sanitizeText_returnsOriginalOnError() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        String result = ollamaService.sanitizeText("original text");

        assertThat(result).isEqualTo("original text");
    }

    @Test
    void generateEmbedding_returnsFloatArray() {
        String responseBody = """
                {
                    "embeddings": [[0.1, 0.2, 0.3, 0.4]]
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        float[] result = ollamaService.generateEmbedding("test text");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);
        assertThat(result[0]).isCloseTo(0.1f, org.assertj.core.data.Offset.offset(0.001f));
    }

    @Test
    void generateEmbedding_returnsNullOnError() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        float[] result = ollamaService.generateEmbedding("test text");

        assertThat(result).isNull();
    }

    @Test
    void interpretQuery_extractsFilters() {
        String responseBody = """
                {
                    "message": {
                        "role": "assistant",
                        "content": "{\\"team\\": \\"Platform\\", \\"severity\\": \\"HIGH\\"}"
                    }
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        Map<String, String> result = ollamaService.interpretQuery("What are the critical issues for Platform?");

        assertThat(result).containsEntry("team", "Platform");
        assertThat(result).containsEntry("severity", "HIGH");
    }

    @Test
    void interpretQuery_handlesEmptyResponse() {
        String responseBody = """
                {
                    "message": {
                        "role": "assistant",
                        "content": "{}"
                    }
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        Map<String, String> result = ollamaService.interpretQuery("Tell me about issues");

        assertThat(result).isEmpty();
    }

    @Test
    void interpretQuery_returnsEmptyMapOnError() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        Map<String, String> result = ollamaService.interpretQuery("some query");

        assertThat(result).isEmpty();
    }

    @Test
    void interpretQuery_handlesMarkdownCodeFences() {
        String responseBody = """
                {
                    "message": {
                        "role": "assistant",
                        "content": "```json\\n{\\"team\\": \\"UX\\"}\\n```"
                    }
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        Map<String, String> result = ollamaService.interpretQuery("UX team issues");

        assertThat(result).containsEntry("team", "UX");
    }

    @Test
    void summarizeReports_returnsSummary() {
        String responseBody = """
                {
                    "message": {
                        "role": "assistant",
                        "content": "The main theme across reports is slow CI pipelines."
                    }
                }
                """;
        mockServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        String result = ollamaService.summarizeReports(
                "What are the issues?",
                List.of("[Tooling | Platform | HIGH] CI is slow", "[Tooling | Platform | MEDIUM] Builds take too long"));

        assertThat(result).isEqualTo("The main theme across reports is slow CI pipelines.");
    }

    @Test
    void summarizeReports_returnsFallbackOnError() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        String result = ollamaService.summarizeReports("question", List.of("report text"));

        assertThat(result).contains("Unable to generate summary");
    }
}
