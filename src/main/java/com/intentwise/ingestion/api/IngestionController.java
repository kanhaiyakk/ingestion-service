package com.intentwise.ingestion.api;

import com.intentwise.ingestion.config.SourceRegistry;
import com.intentwise.ingestion.engine.IngestionEngine;
import com.intentwise.ingestion.run.IngestionRun;
import com.intentwise.ingestion.run.IngestionRunRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST surface for listing sources, triggering ingestion runs, and inspecting run history. */
@Tag(name = "Ingestion")
@RestController
@RequestMapping("/api")
public class IngestionController {

    private final SourceRegistry sourceRegistry;
    private final IngestionEngine engine;
    private final IngestionRunRepository runRepository;
    private final ExecutorService ingestionExecutor;

    public IngestionController(SourceRegistry sourceRegistry, IngestionEngine engine,
            IngestionRunRepository runRepository, ExecutorService ingestionExecutor) {
        this.sourceRegistry = sourceRegistry;
        this.engine = engine;
        this.runRepository = runRepository;
        this.ingestionExecutor = ingestionExecutor;
    }

    @Operation(summary = "List every registered source name")
    @GetMapping("/sources")
    public List<String> listSources() {
        return sourceRegistry.names().stream().sorted().toList();
    }

    @Operation(summary = "Trigger an ingestion run for a source on a background thread")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Run accepted; returns the created, still-RUNNING run"),
            @ApiResponse(responseCode = "404", description = "No source registered with that name")
    })
    @PostMapping("/ingest/{source}")
    public ResponseEntity<RunResponse> ingest(@PathVariable String source) {
        IngestionRun run = engine.start(source);
        ingestionExecutor.execute(() -> engine.execute(run));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RunResponse.from(run));
    }

    @Operation(summary = "List the most recent ingestion runs, newest first")
    @GetMapping("/runs")
    public List<RunResponse> listRuns() {
        return runRepository.findTop50ByOrderByStartedAtDesc().stream().map(RunResponse::from).toList();
    }

    @Operation(summary = "Get one ingestion run by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The run"),
            @ApiResponse(responseCode = "404", description = "No run with that id")
    })
    @GetMapping("/runs/{id}")
    public ResponseEntity<RunResponse> getRun(@PathVariable Long id) {
        return runRepository.findById(id)
                .map(RunResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
