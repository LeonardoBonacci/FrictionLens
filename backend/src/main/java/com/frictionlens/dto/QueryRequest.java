package com.frictionlens.dto;

import com.frictionlens.model.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueryRequest(
        @NotBlank(message = "Question is required")
        @Size(max = 2000, message = "Question must not exceed 2000 characters")
        String question,

        String jobTitle,
        String team,
        String category,
        Severity severity
) {
}
