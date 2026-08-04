package com.intentwise.ingestion.run;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persists {@link IngestionRun}s. */
@Repository
public interface IngestionRunRepository extends JpaRepository<IngestionRun, Long> {
}
