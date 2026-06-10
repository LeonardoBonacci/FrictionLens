package com.frictionlens.controller;

import com.frictionlens.dto.QueryRequest;
import com.frictionlens.dto.QueryResponse;
import com.frictionlens.dto.TrendResponse;
import com.frictionlens.service.QueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        QueryResponse response = queryService.query(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trends")
    public ResponseEntity<TrendResponse> getTrends(
            @RequestParam(defaultValue = "category") String groupBy,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        TrendResponse response = queryService.getTrends(groupBy, from, to);
        return ResponseEntity.ok(response);
    }
}
