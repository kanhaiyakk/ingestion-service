package com.intentwise.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the generic data ingestion service.
 *
 * <p>The service ingests data from arbitrary HTTP APIs described entirely by
 * configuration (see the {@code sources/} directory) and persists it to a
 * pluggable destination. No source-specific code lives in this application.
 */
@SpringBootApplication
public class IngestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionServiceApplication.class, args);
    }
}
