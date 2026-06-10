package com.frictionlens.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frictionlens.dto.QueryRequest;
import com.frictionlens.dto.QueryResponse;
import com.frictionlens.dto.TrendResponse;
import com.frictionlens.model.Severity;
import com.frictionlens.service.QueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QueryService queryService;

    @Test
    void query_validRequest_returnsOk() throws Exception {
        QueryRequest request = new QueryRequest(
                "What are the biggest blockers for the Platform team?",
                null, null, null, null);

        QueryResponse.ReportSnippet snippet = new QueryResponse.ReportSnippet(
                UUID.randomUUID(), "Engineer", "Platform", "Tooling",
                Severity.HIGH, "CI pipeline is slow", Instant.now());

        QueryResponse response = new QueryResponse(
                "The main blocker is CI pipeline performance.", List.of(snippet), 1);

        when(queryService.query(any(QueryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("The main blocker is CI pipeline performance."))
                .andExpect(jsonPath("$.supportingReports").isArray())
                .andExpect(jsonPath("$.supportingReports[0].team").value("Platform"))
                .andExpect(jsonPath("$.totalMatches").value(1));
    }

    @Test
    void query_missingQuestion_returnsBadRequest() throws Exception {
        QueryRequest request = new QueryRequest("", null, null, null, null);

        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.question").exists());
    }

    @Test
    void query_withFilters_returnsOk() throws Exception {
        QueryRequest request = new QueryRequest(
                "What issues exist?", null, "Platform", "Tooling", Severity.HIGH);

        QueryResponse response = new QueryResponse("Summary text", List.of(), 0);
        when(queryService.query(any(QueryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Summary text"))
                .andExpect(jsonPath("$.totalMatches").value(0));
    }

    @Test
    void getTrends_defaultGroupBy_returnsOk() throws Exception {
        TrendResponse response = new TrendResponse("category", List.of(
                new TrendResponse.TrendPoint("Tooling", 10),
                new TrendResponse.TrendPoint("Process", 5)
        ));

        when(queryService.getTrends(eq("category"), isNull(), isNull())).thenReturn(response);

        mockMvc.perform(get("/api/trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("category"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].label").value("Tooling"))
                .andExpect(jsonPath("$.data[0].count").value(10));
    }

    @Test
    void getTrends_withGroupByTeam_returnsOk() throws Exception {
        TrendResponse response = new TrendResponse("team", List.of(
                new TrendResponse.TrendPoint("Platform", 8)
        ));

        when(queryService.getTrends(eq("team"), isNull(), isNull())).thenReturn(response);

        mockMvc.perform(get("/api/trends").param("groupBy", "team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("team"))
                .andExpect(jsonPath("$.data[0].label").value("Platform"));
    }

    @Test
    void getTrends_withDateRange_returnsOk() throws Exception {
        TrendResponse response = new TrendResponse("category", List.of());

        when(queryService.getTrends(eq("category"), any(Instant.class), any(Instant.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/trends")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-06-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("category"));
    }
}
