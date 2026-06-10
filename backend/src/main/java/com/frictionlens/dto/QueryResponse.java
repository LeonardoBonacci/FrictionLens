package com.frictionlens.dto;

import com.frictionlens.model.FrictionReport;
import com.frictionlens.model.Severity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QueryResponse(
        String summary,
        List<ReportSnippet> supportingReports,
        int totalMatches
) {
    public record ReportSnippet(
            UUID id,
            String jobTitle,
            String team,
            String category,
            Severity severity,
            String blockerText,
            Instant createdAt
    ) {
        public static ReportSnippet from(FrictionReport report) {
            return new ReportSnippet(
                    report.getId(),
                    report.getJobTitle(),
                    report.getTeam(),
                    report.getCategory(),
                    report.getSeverity(),
                    report.getBlockerText(),
                    report.getCreatedAt()
            );
        }
    }
}
