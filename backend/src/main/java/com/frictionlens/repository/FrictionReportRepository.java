package com.frictionlens.repository;

import com.frictionlens.model.FrictionReport;
import com.frictionlens.model.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FrictionReportRepository extends JpaRepository<FrictionReport, UUID> {

    Page<FrictionReport> findByJobTitle(String jobTitle, Pageable pageable);

    Page<FrictionReport> findByTeam(String team, Pageable pageable);

    Page<FrictionReport> findByCategory(String category, Pageable pageable);

    Page<FrictionReport> findBySeverity(Severity severity, Pageable pageable);

    Page<FrictionReport> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);

    @Query("""
            SELECT r FROM FrictionReport r
            WHERE (:jobTitle IS NULL OR r.jobTitle = :jobTitle)
              AND (:team IS NULL OR r.team = :team)
              AND (:category IS NULL OR r.category = :category)
              AND (:severity IS NULL OR r.severity = :severity)
              AND (:from IS NULL OR r.createdAt >= :from)
              AND (:to IS NULL OR r.createdAt <= :to)
            """)
    Page<FrictionReport> findWithFilters(
            @Param("jobTitle") String jobTitle,
            @Param("team") String team,
            @Param("category") String category,
            @Param("severity") Severity severity,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query(value = """
            SELECT * FROM friction_reports
            WHERE embedding IS NOT NULL
            ORDER BY embedding <-> cast(:queryVector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<FrictionReport> findNearestByEmbedding(String queryVector, int limit);
}
