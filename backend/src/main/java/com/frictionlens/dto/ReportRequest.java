package com.frictionlens.dto;

import com.frictionlens.model.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotBlank(message = "Job title is required")
        @Size(max = 255, message = "Job title must not exceed 255 characters")
        String jobTitle,

        @NotBlank(message = "Team is required")
        @Size(max = 255, message = "Team must not exceed 255 characters")
        String team,

        @NotBlank(message = "Category is required")
        @Size(max = 255, message = "Category must not exceed 255 characters")
        String category,

        @NotNull(message = "Severity is required")
        Severity severity,

        @NotBlank(message = "Blocker text is required")
        @Size(max = 10000, message = "Blocker text must not exceed 10000 characters")
        String blockerText
) {
}
