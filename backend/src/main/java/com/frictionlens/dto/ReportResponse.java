package com.frictionlens.dto;

import com.frictionlens.model.FrictionReport;
import com.frictionlens.model.Severity;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String jobTitle,
        String team,
        String category,
        Severity severity,
        String blockerText,
        Instant createdAt
) {
    public static ReportResponse from(FrictionReport report) {
        return new ReportResponse(
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
