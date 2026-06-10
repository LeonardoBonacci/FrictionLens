package com.frictionlens.service;

import com.frictionlens.dto.QueryRequest;
import com.frictionlens.dto.QueryResponse;
import com.frictionlens.dto.TrendResponse;
import com.frictionlens.model.FrictionReport;
import com.frictionlens.model.Severity;
import com.frictionlens.repository.FrictionReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private FrictionReportRepository reportRepository;

    @Mock
    private OllamaService ollamaService;

    @InjectMocks
    private QueryService queryService;

    @Test
    void query_withEmbedding_performsHybridSearch() {
        QueryRequest request = new QueryRequest(
                "What are the biggest blockers?", null, null, null, null);

        when(ollamaService.interpretQuery(anyString())).thenReturn(Collections.emptyMap());
        when(ollamaService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f, 0.2f});

        FrictionReport report = createReport("Tooling", "Platform", Severity.HIGH, "CI is slow");
        when(reportRepository.findHybridSearch(anyString(), isNull(), isNull(), isNull(), isNull(), eq(20)))
                .thenReturn(List.of(report));

        when(ollamaService.summarizeReports(anyString(), any())).thenReturn("CI pipeline is the main issue.");

        QueryResponse response = queryService.query(request);

        assertThat(response.summary()).isEqualTo("CI pipeline is the main issue.");
        assertThat(response.supportingReports()).hasSize(1);
        assertThat(response.supportingReports().getFirst().category()).isEqualTo("Tooling");
        assertThat(response.totalMatches()).isEqualTo(1);

        verify(reportRepository).findHybridSearch(anyString(), isNull(), isNull(), isNull(), isNull(), eq(20));
    }

    @Test
    void query_withoutEmbedding_fallsBackToFilteredSearch() {
        QueryRequest request = new QueryRequest(
                "Show me all reports", null, "Platform", null, null);

        when(ollamaService.interpretQuery(anyString())).thenReturn(Collections.emptyMap());
        when(ollamaService.generateEmbedding(anyString())).thenReturn(null);

        FrictionReport report = createReport("Tooling", "Platform", Severity.MEDIUM, "Build issues");
        when(reportRepository.findWithFilters(isNull(), eq("Platform"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));

        when(ollamaService.summarizeReports(anyString(), any())).thenReturn("Build related issues found.");

        QueryResponse response = queryService.query(request);

        assertThat(response.summary()).isEqualTo("Build related issues found.");
        assertThat(response.supportingReports()).hasSize(1);
        verify(reportRepository).findWithFilters(isNull(), eq("Platform"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void query_interpretedFiltersApplied() {
        QueryRequest request = new QueryRequest(
                "What issues does the UX team have?", null, null, null, null);

        when(ollamaService.interpretQuery(anyString()))
                .thenReturn(Map.of("team", "UX"));
        when(ollamaService.generateEmbedding(anyString())).thenReturn(new float[]{0.5f});

        when(reportRepository.findHybridSearch(anyString(), isNull(), eq("UX"), isNull(), isNull(), eq(20)))
                .thenReturn(List.of());

        QueryResponse response = queryService.query(request);

        assertThat(response.summary()).isEqualTo("No matching reports found for your query.");
        assertThat(response.totalMatches()).isEqualTo(0);
        verify(reportRepository).findHybridSearch(anyString(), isNull(), eq("UX"), isNull(), isNull(), eq(20));
    }

    @Test
    void query_explicitFiltersTakePrecedenceOverInterpreted() {
        QueryRequest request = new QueryRequest(
                "What issues exist?", null, "Platform", null, null);

        when(ollamaService.interpretQuery(anyString()))
                .thenReturn(Map.of("team", "UX"));
        when(ollamaService.generateEmbedding(anyString())).thenReturn(new float[]{0.5f});

        when(reportRepository.findHybridSearch(anyString(), isNull(), eq("Platform"), isNull(), isNull(), eq(20)))
                .thenReturn(List.of());

        queryService.query(request);

        // Explicit "Platform" should take precedence over interpreted "UX"
        verify(reportRepository).findHybridSearch(anyString(), isNull(), eq("Platform"), isNull(), isNull(), eq(20));
    }

    @Test
    void query_noResults_returnsNoMatchMessage() {
        QueryRequest request = new QueryRequest("Anything about security?", null, null, null, null);

        when(ollamaService.interpretQuery(anyString())).thenReturn(Collections.emptyMap());
        when(ollamaService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(reportRepository.findHybridSearch(anyString(), isNull(), isNull(), isNull(), isNull(), eq(20)))
                .thenReturn(List.of());

        QueryResponse response = queryService.query(request);

        assertThat(response.summary()).isEqualTo("No matching reports found for your query.");
        assertThat(response.supportingReports()).isEmpty();
    }

    @Test
    void getTrends_byCategory_returnsCounts() {
        List<Object[]> data = List.of(
                new Object[]{"Tooling", 10L},
                new Object[]{"Process", 5L}
        );
        when(reportRepository.countByCategory(isNull(), isNull())).thenReturn(data);

        TrendResponse response = queryService.getTrends("category", null, null);

        assertThat(response.groupBy()).isEqualTo("category");
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().getFirst().label()).isEqualTo("Tooling");
        assertThat(response.data().getFirst().count()).isEqualTo(10);
    }

    @Test
    void getTrends_byTeam_returnsCounts() {
        List<Object[]> data = List.<Object[]>of(new Object[]{"Platform", 8L});
        when(reportRepository.countByTeam(isNull(), isNull())).thenReturn(data);

        TrendResponse response = queryService.getTrends("team", null, null);

        assertThat(response.groupBy()).isEqualTo("team");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().label()).isEqualTo("Platform");
    }

    @Test
    void getTrends_bySeverity_returnsCounts() {
        List<Object[]> data = List.of(
                new Object[]{"HIGH", 7L},
                new Object[]{"LOW", 3L}
        );
        when(reportRepository.countBySeverity(isNull(), isNull())).thenReturn(data);

        TrendResponse response = queryService.getTrends("severity", null, null);

        assertThat(response.groupBy()).isEqualTo("severity");
        assertThat(response.data()).hasSize(2);
    }

    @Test
    void getTrends_byJobTitle_returnsCounts() {
        List<Object[]> data = List.<Object[]>of(new Object[]{"Software Engineer", 12L});
        when(reportRepository.countByJobTitle(isNull(), isNull())).thenReturn(data);

        TrendResponse response = queryService.getTrends("jobTitle", null, null);

        assertThat(response.groupBy()).isEqualTo("jobTitle");
        assertThat(response.data().getFirst().label()).isEqualTo("Software Engineer");
    }

    @Test
    void getTrends_withDateRange_passesFilterToRepository() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-01T00:00:00Z");

        when(reportRepository.countByCategory(eq(from), eq(to)))
                .thenReturn(List.of());

        TrendResponse response = queryService.getTrends("category", from, to);

        assertThat(response.data()).isEmpty();
        verify(reportRepository).countByCategory(from, to);
    }

    @Test
    void getTrends_unknownGroupBy_defaultsToCategory() {
        when(reportRepository.countByCategory(isNull(), isNull()))
                .thenReturn(List.of());

        TrendResponse response = queryService.getTrends("unknown", null, null);

        assertThat(response.groupBy()).isEqualTo("unknown");
        verify(reportRepository).countByCategory(isNull(), isNull());
    }

    private FrictionReport createReport(String category, String team, Severity severity, String text) {
        FrictionReport report = new FrictionReport();
        report.setId(UUID.randomUUID());
        report.setJobTitle("Engineer");
        report.setTeam(team);
        report.setCategory(category);
        report.setSeverity(severity);
        report.setBlockerText(text);
        report.setCreatedAt(Instant.now());
        return report;
    }
}
