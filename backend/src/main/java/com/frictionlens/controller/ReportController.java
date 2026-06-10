package com.frictionlens.controller;

import com.frictionlens.dto.ReportRequest;
import com.frictionlens.dto.ReportResponse;
import com.frictionlens.model.Severity;
import com.frictionlens.service.FrictionReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final FrictionReportService reportService;

    public ReportController(FrictionReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ReportResponse> submitReport(@Valid @RequestBody ReportRequest request) {
        ReportResponse response = reportService.submitReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ReportResponse>> listReports(
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReportResponse> reports = reportService.listReports(jobTitle, team, category, severity, from, to, pageable);
        return ResponseEntity.ok(reports);
    }
}
