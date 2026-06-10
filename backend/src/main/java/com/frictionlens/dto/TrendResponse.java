package com.frictionlens.dto;

import java.util.List;

public record TrendResponse(
        String groupBy,
        List<TrendPoint> data
) {
    public record TrendPoint(
            String label,
            long count
    ) {
    }
}
