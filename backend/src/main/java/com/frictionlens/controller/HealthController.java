package com.frictionlens.controller;

import com.frictionlens.service.OllamaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final OllamaService ollamaService;

    public HealthController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        boolean ollamaUp = ollamaService.isAvailable();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "ollama", ollamaUp ? "available" : "unavailable"
        ));
    }
}
