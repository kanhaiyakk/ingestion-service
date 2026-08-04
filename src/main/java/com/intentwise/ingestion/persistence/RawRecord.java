package com.intentwise.ingestion.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One ingested record: a source-scoped idempotency key plus its raw JSON
 * payload. The unique constraint on (source, recordKey) is what
 * {@link RawRecordRepository#upsert} relies on to stay idempotent across
 * re-runs.
 */
@Entity
@Table(name = "raw_records", uniqueConstraints = @UniqueConstraint(columnNames = {"source", "record_key"}))
public class RawRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Column(name = "record_key", nullable = false)
    private String recordKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    protected RawRecord() {
    }

    public RawRecord(String source, String recordKey, String payload, Long runId, Instant ingestedAt) {
        this.source = source;
        this.recordKey = recordKey;
        this.payload = payload;
        this.runId = runId;
        this.ingestedAt = ingestedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public String getPayload() {
        return payload;
    }

    public Long getRunId() {
        return runId;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
