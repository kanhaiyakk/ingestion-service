package com.intentwise.ingestion.run;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One ingestion run for a source: lifecycle status, counters, and timing. */
@Entity
@Table(name = "ingestion_runs")
public class IngestionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestionRunStatus status;

    @Column(name = "records_written", nullable = false)
    private long recordsWritten;

    @Column(name = "pages_fetched", nullable = false)
    private int pagesFetched;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message")
    private String errorMessage;

    protected IngestionRun() {
    }

    public IngestionRun(String source, IngestionRunStatus status, Instant startedAt) {
        this.source = source;
        this.status = status;
        this.startedAt = startedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public IngestionRunStatus getStatus() {
        return status;
    }

    public void setStatus(IngestionRunStatus status) {
        this.status = status;
    }

    public long getRecordsWritten() {
        return recordsWritten;
    }

    public void setRecordsWritten(long recordsWritten) {
        this.recordsWritten = recordsWritten;
    }

    public int getPagesFetched() {
        return pagesFetched;
    }

    public void setPagesFetched(int pagesFetched) {
        this.pagesFetched = pagesFetched;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
