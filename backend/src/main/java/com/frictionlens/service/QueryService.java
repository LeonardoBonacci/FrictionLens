package com.frictionlens.service;

import com.frictionlens.dto.QueryRequest;
import com.frictionlens.dto.QueryResponse;
import com.frictionlens.dto.TrendResponse;
import com.frictionlens.model.FrictionReport;
import com.frictionlens.model.Severity;
import com.frictionlens.repository.FrictionReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);
    private static final int MAX_RESULTS = 20;

    private final FrictionReportRepository reportRepository;
    private final OllamaService ollamaService;

    public QueryService(FrictionReportRepository reportRepository, OllamaService ollamaService) {
        this.reportRepository = reportRepository;
        this.ollamaService = ollamaService;
    }

    public QueryResponse query(QueryRequest request) {
        // 1. Interpret query to extract structured filters
        String jobTitle = request.jobTitle();
        String team = request.team();
        String category = request.category();
        Severity severity = request.severity();

        Map<String, String> interpreted = ollamaService.interpretQuery(request.question());
        log.info("Interpreted query filters: {}", interpreted);

        if (jobTitle == null) jobTitle = interpreted.get("jobTitle");
        if (team == null) team = interpreted.get("team");
        if (category == null) category = interpreted.get("category");
        if (severity == null && interpreted.containsKey("severity")) {
            try {
                severity = Severity.valueOf(interpreted.get("severity"));
            } catch (IllegalArgumentException ignored) {
                // Invalid severity from LLM, skip
            }
        }

        // 2. Generate embedding for the question
        float[] embedding = ollamaService.generateEmbedding(request.question());

        // 3. Hybrid search: semantic similarity + structured filters
        List<FrictionReport> results;
        if (embedding != null) {
            String vectorString = vectorToString(embedding);
            results = reportRepository.findHybridSearch(
                    vectorString, jobTitle, team, category,
                    severity != null ? severity.name() : null,
                    MAX_RESULTS
            );
            log.info("Hybrid search returned {} results", results.size());
        } else {
            // Fallback to filtered search without embedding
            log.warn("Embedding generation failed, falling back to filtered search");
            results = reportRepository.findWithFilters(
                    jobTitle, team, category,
                    severity != null ? severity.name() : null, null, null,
                    PageRequest.of(0, MAX_RESULTS)
            ).getContent();
        }

        // 4. Cluster matching reports by category for summarization context
        List<String> reportTexts = results.stream()
                .map(r -> String.format("[%s | %s | %s] %s",
                        r.getCategory(), r.getTeam(), r.getSeverity(), r.getBlockerText()))
                .toList();

        // 5. Summarize clustered results via Ollama
        String summary;
        if (results.isEmpty()) {
            summary = "No matching reports found for your query.";
        } else {
            summary = ollamaService.summarizeReports(request.question(), reportTexts);
        }

        // 6. Build response with supporting snippets
        List<QueryResponse.ReportSnippet> snippets = results.stream()
                .map(QueryResponse.ReportSnippet::from)
                .toList();

        return new QueryResponse(summary, snippets, results.size());
    }

    public TrendResponse getTrends(String groupBy, Instant from, Instant to) {
        List<Object[]> rawData = switch (groupBy) {
            case "team" -> reportRepository.countByTeam(from, to);
            case "severity" -> reportRepository.countBySeverity(from, to);
            case "jobTitle" -> reportRepository.countByJobTitle(from, to);
            default -> reportRepository.countByCategory(from, to);
        };

        List<TrendResponse.TrendPoint> data = rawData.stream()
                .map(row -> new TrendResponse.TrendPoint(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();

        return new TrendResponse(groupBy, data);
    }

    private String vectorToString(float[] vector) {
        return Arrays.toString(vector);
    }
}
