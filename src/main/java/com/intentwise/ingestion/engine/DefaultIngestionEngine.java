package com.intentwise.ingestion.engine;

import com.intentwise.ingestion.auth.AuthStrategy;
import com.intentwise.ingestion.auth.AuthStrategyRegistry;
import com.intentwise.ingestion.config.SourceConfig;
import com.intentwise.ingestion.config.SourceRegistry;
import com.intentwise.ingestion.extract.ExtractedRecord;
import com.intentwise.ingestion.extract.RecordExtractor;
import com.intentwise.ingestion.http.FetchResponse;
import com.intentwise.ingestion.http.HttpFetcher;
import com.intentwise.ingestion.http.PageRequest;
import com.intentwise.ingestion.http.RateLimitingHttpFetcher;
import com.intentwise.ingestion.pagination.PaginationContext;
import com.intentwise.ingestion.pagination.Paginator;
import com.intentwise.ingestion.pagination.PaginatorRegistry;
import com.intentwise.ingestion.run.IngestionRun;
import com.intentwise.ingestion.run.IngestionRunRepository;
import com.intentwise.ingestion.run.IngestionRunStatus;
import com.intentwise.ingestion.sink.Sink;
import com.intentwise.ingestion.sink.SinkRegistry;
import com.intentwise.ingestion.sink.WriteContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orchestrates one ingestion run: resolves the source's auth, pagination,
 * and sink strategies, drives the paginate-fetch-extract-write loop until
 * the paginator signals done, and records the outcome on {@link IngestionRun}.
 */
@Component
public class DefaultIngestionEngine implements IngestionEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultIngestionEngine.class);

    private final SourceRegistry sourceRegistry;
    private final AuthStrategyRegistry authStrategyRegistry;
    private final PaginatorRegistry paginatorRegistry;
    private final SinkRegistry sinkRegistry;
    private final RecordExtractor recordExtractor;
    private final HttpFetcher httpFetcher;
    private final IngestionRunRepository runRepository;

    public DefaultIngestionEngine(SourceRegistry sourceRegistry, AuthStrategyRegistry authStrategyRegistry,
            PaginatorRegistry paginatorRegistry, SinkRegistry sinkRegistry, RecordExtractor recordExtractor,
            HttpFetcher httpFetcher, IngestionRunRepository runRepository) {
        this.sourceRegistry = sourceRegistry;
        this.authStrategyRegistry = authStrategyRegistry;
        this.paginatorRegistry = paginatorRegistry;
        this.sinkRegistry = sinkRegistry;
        this.recordExtractor = recordExtractor;
        this.httpFetcher = httpFetcher;
        this.runRepository = runRepository;
    }

    @Override
    public IngestionRun run(String sourceName) {
        IngestionRun run = start(sourceName);
        execute(run);
        return run;
    }

    @Override
    public IngestionRun start(String sourceName) {
        sourceRegistry.get(sourceName);
        IngestionRun run = runRepository.saveAndFlush(new IngestionRun(sourceName, IngestionRunStatus.RUNNING, Instant.now()));
        log.info("Starting ingestion run {} for source '{}'", run.getId(), sourceName);
        return run;
    }

    @Override
    public void execute(IngestionRun run) {
        String sourceName = run.getSource();
        SourceConfig source = sourceRegistry.get(sourceName);

        int pagesFetched = 0;
        long recordsWritten = 0;
        try {
            AuthStrategy auth = authStrategyRegistry.get(source.auth().type());
            Paginator paginator = paginatorRegistry.get(source.pagination().type());
            Sink sink = sinkRegistry.get(source.sink().type());
            HttpFetcher fetcher = RateLimitingHttpFetcher.wrap(httpFetcher, source.rateLimit());

            FetchResponse lastResponse = null;
            Optional<PageRequest> next = paginator.next(new PaginationContext(source, null, pagesFetched));

            while (next.isPresent()) {
                PageRequest authedRequest = auth.apply(next.get(), source.auth());
                FetchResponse response = fetcher.fetch(authedRequest);
                pagesFetched++;

                List<ExtractedRecord> records = recordExtractor.extract(response, source.extract());
                sink.write(new WriteContext(sourceName, run.getId(), records));
                recordsWritten += records.size();

                log.debug("Run {} page {}: fetched {} record(s) for source '{}'",
                        run.getId(), pagesFetched, records.size(), sourceName);

                lastResponse = response;
                next = paginator.next(new PaginationContext(source, lastResponse, pagesFetched));
            }

            run.setStatus(IngestionRunStatus.SUCCESS);
            run.setPagesFetched(pagesFetched);
            run.setRecordsWritten(recordsWritten);
            run.setFinishedAt(Instant.now());
            runRepository.saveAndFlush(run);

            log.info("Run {} for source '{}' succeeded: {} page(s), {} record(s)",
                    run.getId(), sourceName, pagesFetched, recordsWritten);
        } catch (Exception e) {
            run.setStatus(IngestionRunStatus.FAILED);
            run.setPagesFetched(pagesFetched);
            run.setRecordsWritten(recordsWritten);
            run.setErrorMessage(e.getMessage());
            run.setFinishedAt(Instant.now());
            runRepository.saveAndFlush(run);

            log.error("Run {} for source '{}' failed after {} page(s): {}",
                    run.getId(), sourceName, pagesFetched, e.getMessage());
            throw new IngestionEngineException(
                    "Ingestion run " + run.getId() + " for source '" + sourceName + "' failed: " + e.getMessage(), e);
        }
    }
}
