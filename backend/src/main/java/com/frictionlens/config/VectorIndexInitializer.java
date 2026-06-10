package com.frictionlens.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VectorIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorIndexInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public VectorIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_embedding_hnsw
                    ON friction_reports
                    USING hnsw (embedding vector_cosine_ops)
                    """);
            log.info("pgvector HNSW index on embedding column ensured");
        } catch (Exception e) {
            log.warn("Could not create HNSW index on embedding column: {}", e.getMessage());
        }
    }
}
