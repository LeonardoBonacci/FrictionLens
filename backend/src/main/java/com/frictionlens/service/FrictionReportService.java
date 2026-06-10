package com.frictionlens.service;

import com.frictionlens.dto.ReportRequest;
import com.frictionlens.dto.ReportResponse;
import com.frictionlens.model.FrictionReport;
import com.frictionlens.model.Severity;
import com.frictionlens.repository.FrictionReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class FrictionReportService {

    private static final Logger log = LoggerFactory.getLogger(FrictionReportService.class);

    private final FrictionReportRepository reportRepository;
    private final OllamaService ollamaService;

    public FrictionReportService(FrictionReportRepository reportRepository, OllamaService ollamaService) {
        this.reportRepository = reportRepository;
        this.ollamaService = ollamaService;
    }

    public ReportResponse submitReport(ReportRequest request) {
        String sanitizedText = ollamaService.sanitizeText(request.blockerText());
        log.info("Blocker text sanitized for privacy");

        float[] embedding = ollamaService.generateEmbedding(sanitizedText);
        if (embedding != null) {
            log.info("Generated embedding with {} dimensions", embedding.length);
        } else {
            log.warn("Embedding generation returned null, report will be stored without embedding");
        }

        FrictionReport report = new FrictionReport();
        report.setJobTitle(request.jobTitle());
        report.setTeam(request.team());
        report.setCategory(request.category());
        report.setSeverity(request.severity());
        report.setBlockerText(sanitizedText);
        report.setEmbedding(embedding);

        FrictionReport saved = reportRepository.save(report);
        log.info("Friction report saved with id={}", saved.getId());

        return ReportResponse.from(saved);
    }

    public Page<ReportResponse> listReports(String jobTitle, String team, String category,
                                            Severity severity, Instant from, Instant to,
                                            Pageable pageable) {
        // Strip sort from pageable for native query (sort is handled in the query itself)
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        String severityStr = severity != null ? severity.name() : null;
        return reportRepository.findWithFilters(jobTitle, team, category, severityStr, from, to, unsorted)
                .map(ReportResponse::from);
    }
}
