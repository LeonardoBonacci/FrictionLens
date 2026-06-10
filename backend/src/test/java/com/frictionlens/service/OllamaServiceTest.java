package com.frictionlens.service;

import com.frictionlens.config.OllamaConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

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

        ollamaService = new OllamaService(config, WebClient.builder());
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
}
