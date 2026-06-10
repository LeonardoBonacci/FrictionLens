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

    @Query(value = """
            SELECT * FROM friction_reports
            WHERE (cast(:jobTitle as text) IS NULL OR job_title = :jobTitle)
              AND (cast(:team as text) IS NULL OR team = :team)
              AND (cast(:category as text) IS NULL OR category = :category)
              AND (cast(:severity as text) IS NULL OR severity = cast(:severity as text))
              AND (cast(:from as timestamptz) IS NULL OR created_at >= cast(:from as timestamptz))
              AND (cast(:to as timestamptz) IS NULL OR created_at <= cast(:to as timestamptz))
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM friction_reports
            WHERE (cast(:jobTitle as text) IS NULL OR job_title = :jobTitle)
              AND (cast(:team as text) IS NULL OR team = :team)
              AND (cast(:category as text) IS NULL OR category = :category)
              AND (cast(:severity as text) IS NULL OR severity = cast(:severity as text))
              AND (cast(:from as timestamptz) IS NULL OR created_at >= cast(:from as timestamptz))
              AND (cast(:to as timestamptz) IS NULL OR created_at <= cast(:to as timestamptz))
            """,
            nativeQuery = true)
    Page<FrictionReport> findWithFilters(
            @Param("jobTitle") String jobTitle,
            @Param("team") String team,
            @Param("category") String category,
            @Param("severity") String severity,
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

    @Query(value = """
            SELECT * FROM friction_reports
            WHERE embedding IS NOT NULL
              AND (:jobTitle IS NULL OR job_title = :jobTitle)
              AND (:team IS NULL OR team = :team)
              AND (:category IS NULL OR category = :category)
              AND (cast(:severity as text) IS NULL OR severity = cast(:severity as text))
            ORDER BY embedding <-> cast(:queryVector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<FrictionReport> findHybridSearch(
            @Param("queryVector") String queryVector,
            @Param("jobTitle") String jobTitle,
            @Param("team") String team,
            @Param("category") String category,
            @Param("severity") String severity,
            @Param("limit") int limit
    );

    @Query(value = "SELECT team, COUNT(*) FROM friction_reports " +
            "WHERE (cast(:from as timestamptz) IS NULL OR created_at >= cast(:from as timestamptz)) " +
            "AND (cast(:to as timestamptz) IS NULL OR created_at <= cast(:to as timestamptz)) " +
            "GROUP BY team ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countByTeam(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "SELECT category, COUNT(*) FROM friction_reports " +
            "WHERE (cast(:from as timestamptz) IS NULL OR created_at >= cast(:from as timestamptz)) " +
            "AND (cast(:to as timestamptz) IS NULL OR created_at <= cast(:to as timestamptz)) " +
            "GROUP BY category ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countByCategory(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "SELECT severity, COUNT(*) FROM friction_reports " +
            "WHERE (cast(:from as timestamptz) IS NULL OR created_at >= cast(:from as timestamptz)) " +
            "AND (cast(:to as timestamptz) IS NULL OR created_at <= cast(:to as timestamptz)) " +
            "GROUP BY severity ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countBySeverity(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "SELECT job_title, COUNT(*) FROM friction_reports " +
            "WHERE (cast(:from as timestamptz) IS NULL OR created_at >= cast(:from as timestamptz)) " +
            "AND (cast(:to as timestamptz) IS NULL OR created_at <= cast(:to as timestamptz)) " +
            "GROUP BY job_title ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countByJobTitle(@Param("from") Instant from, @Param("to") Instant to);
}
