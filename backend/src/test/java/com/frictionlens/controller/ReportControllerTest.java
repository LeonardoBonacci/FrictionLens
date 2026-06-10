package com.frictionlens.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frictionlens.dto.ReportRequest;
import com.frictionlens.dto.ReportResponse;
import com.frictionlens.model.Severity;
import com.frictionlens.service.FrictionReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FrictionReportService reportService;

    @Test
    void submitReport_validRequest_returnsCreated() throws Exception {
        ReportRequest request = new ReportRequest(
                "Software Engineer", "Platform", "Tooling", Severity.HIGH,
                "The CI pipeline takes 45 minutes to run");

        ReportResponse response = new ReportResponse(
                UUID.randomUUID(), "Software Engineer", "Platform", "Tooling",
                Severity.HIGH, "The CI pipeline takes 45 minutes to run", Instant.now());

        when(reportService.submitReport(any(ReportRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.blockerText").value("The CI pipeline takes 45 minutes to run"));
    }

    @Test
    void submitReport_missingJobTitle_returnsBadRequest() throws Exception {
        ReportRequest request = new ReportRequest(
                "", "Platform", "Tooling", Severity.HIGH, "Some blocker text");

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jobTitle").exists());
    }

    @Test
    void submitReport_missingSeverity_returnsBadRequest() throws Exception {
        String json = """
                {
                    "jobTitle": "Engineer",
                    "team": "Platform",
                    "category": "Tooling",
                    "blockerText": "Some blocker"
                }
                """;

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.severity").exists());
    }

    @Test
    void submitReport_missingBlockerText_returnsBadRequest() throws Exception {
        ReportRequest request = new ReportRequest(
                "Engineer", "Platform", "Tooling", Severity.LOW, "");

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.blockerText").exists());
    }

    @Test
    void listReports_noFilters_returnsPage() throws Exception {
        ReportResponse response = new ReportResponse(
                UUID.randomUUID(), "Engineer", "Platform", "Tooling",
                Severity.MEDIUM, "Sanitized text", Instant.now());

        Page<ReportResponse> page = new PageImpl<>(List.of(response));
        when(reportService.listReports(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].jobTitle").value("Engineer"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listReports_withFilters_returnsFilteredPage() throws Exception {
        ReportResponse response = new ReportResponse(
                UUID.randomUUID(), "Designer", "UX", "Process",
                Severity.HIGH, "Filtered report", Instant.now());

        Page<ReportResponse> page = new PageImpl<>(List.of(response));
        when(reportService.listReports(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/reports")
                        .param("team", "UX")
                        .param("severity", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].team").value("UX"));
    }
}
