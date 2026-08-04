package com.intentwise.ingestion.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Persists {@link RawRecord}s. {@link #upsert} is the idempotent write path:
 * a native {@code INSERT ... ON CONFLICT} so re-ingesting the same
 * (source, recordKey) updates the existing row instead of duplicating it.
 */
@Repository
public interface RawRecordRepository extends JpaRepository<RawRecord, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO raw_records (source, record_key, payload, run_id, ingested_at)
            VALUES (:source, :recordKey, CAST(:payload AS jsonb), :runId, :ingestedAt)
            ON CONFLICT (source, record_key)
            DO UPDATE SET payload = EXCLUDED.payload, run_id = EXCLUDED.run_id, ingested_at = EXCLUDED.ingested_at
            """, nativeQuery = true)
    void upsert(@Param("source") String source, @Param("recordKey") String recordKey,
            @Param("payload") String payload, @Param("runId") Long runId, @Param("ingestedAt") Instant ingestedAt);

    List<RawRecord> findBySource(String source);

    Optional<RawRecord> findBySourceAndRecordKey(String source, String recordKey);
}
