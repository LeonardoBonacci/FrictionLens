package com.frictionlens.service;

import com.frictionlens.dto.ReportRequest;
import com.frictionlens.dto.ReportResponse;
import com.frictionlens.model.FrictionReport;
import com.frictionlens.model.Severity;
import com.frictionlens.repository.FrictionReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FrictionReportServiceTest {

    @Mock
    private FrictionReportRepository reportRepository;

    @Mock
    private OllamaService ollamaService;

    @InjectMocks
    private FrictionReportService reportService;

    @Test
    void submitReport_sanitizesTextAndGeneratesEmbedding() {
        ReportRequest request = new ReportRequest(
                "Software Engineer", "Platform", "Tooling", Severity.HIGH,
                "John Smith's email john@example.com caused issues with the CI pipeline");

        when(ollamaService.sanitizeText(any())).thenReturn("A colleague's email [email removed] caused issues with the CI pipeline");
        when(ollamaService.generateEmbedding(any())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        FrictionReport savedReport = new FrictionReport();
        savedReport.setId(UUID.randomUUID());
        savedReport.setJobTitle("Software Engineer");
        savedReport.setTeam("Platform");
        savedReport.setCategory("Tooling");
        savedReport.setSeverity(Severity.HIGH);
        savedReport.setBlockerText("A colleague's email [email removed] caused issues with the CI pipeline");
        savedReport.setEmbedding(new float[]{0.1f, 0.2f, 0.3f});
        savedReport.setCreatedAt(Instant.now());

        when(reportRepository.save(any(FrictionReport.class))).thenReturn(savedReport);

        ReportResponse response = reportService.submitReport(request);

        assertThat(response.blockerText()).isEqualTo("A colleague's email [email removed] caused issues with the CI pipeline");
        assertThat(response.jobTitle()).isEqualTo("Software Engineer");
        assertThat(response.severity()).isEqualTo(Severity.HIGH);

        ArgumentCaptor<FrictionReport> captor = ArgumentCaptor.forClass(FrictionReport.class);
        verify(reportRepository).save(captor.capture());
        FrictionReport captured = captor.getValue();
        assertThat(captured.getBlockerText()).doesNotContain("John Smith");
        assertThat(captured.getEmbedding()).isNotNull();
    }

    @Test
    void submitReport_handlesNullEmbedding() {
        ReportRequest request = new ReportRequest(
                "Designer", "UX", "Process", Severity.MEDIUM, "Design reviews are slow");

        when(ollamaService.sanitizeText(any())).thenReturn("Design reviews are slow");
        when(ollamaService.generateEmbedding(any())).thenReturn(null);

        FrictionReport savedReport = new FrictionReport();
        savedReport.setId(UUID.randomUUID());
        savedReport.setJobTitle("Designer");
        savedReport.setTeam("UX");
        savedReport.setCategory("Process");
        savedReport.setSeverity(Severity.MEDIUM);
        savedReport.setBlockerText("Design reviews are slow");
        savedReport.setCreatedAt(Instant.now());

        when(reportRepository.save(any(FrictionReport.class))).thenReturn(savedReport);

        ReportResponse response = reportService.submitReport(request);

        assertThat(response).isNotNull();
        assertThat(response.blockerText()).isEqualTo("Design reviews are slow");

        ArgumentCaptor<FrictionReport> captor = ArgumentCaptor.forClass(FrictionReport.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getEmbedding()).isNull();
    }

    @Test
    void listReports_delegatesToRepository() {
        FrictionReport report = new FrictionReport();
        report.setId(UUID.randomUUID());
        report.setJobTitle("Engineer");
        report.setTeam("Platform");
        report.setCategory("Tooling");
        report.setSeverity(Severity.LOW);
        report.setBlockerText("Some text");
        report.setCreatedAt(Instant.now());

        Page<FrictionReport> page = new PageImpl<>(List.of(report));
        Pageable pageable = PageRequest.of(0, 20);

        when(reportRepository.findWithFilters(null, "Platform", null, null, null, null, pageable))
                .thenReturn(page);

        Page<ReportResponse> result = reportService.listReports(null, "Platform", null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().team()).isEqualTo("Platform");
    }
}
