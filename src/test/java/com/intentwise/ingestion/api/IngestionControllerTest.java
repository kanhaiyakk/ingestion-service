package com.intentwise.ingestion.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.intentwise.ingestion.config.SourceConfigException;
import com.intentwise.ingestion.config.SourceRegistry;
import com.intentwise.ingestion.engine.IngestionEngine;
import com.intentwise.ingestion.run.IngestionRun;
import com.intentwise.ingestion.run.IngestionRunRepository;
import com.intentwise.ingestion.run.IngestionRunStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice test for {@link IngestionController} (web layer only, no real engine
 * or database): unknown source is 404, a known source returns 202 with the
 * created run id, /api/runs lists runs, plus /api/sources and /api/runs/{id}.
 */
@WebMvcTest(IngestionController.class)
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SourceRegistry sourceRegistry;

    @MockBean
    private IngestionEngine engine;

    @MockBean
    private IngestionRunRepository runRepository;

    @MockBean
    private ExecutorService ingestionExecutor;

    @Test
    void triggeringUnknownSourceReturns404() throws Exception {
        given(engine.start("unknown")).willThrow(new SourceConfigException("No source registered with name 'unknown'"));

        mockMvc.perform(post("/api/ingest/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No source registered with name 'unknown'"));
    }

    @Test
    void triggeringKnownSourceReturns202WithRunId() throws Exception {
        IngestionRun run = runWithId(99L, "pokeapi", IngestionRunStatus.RUNNING);
        given(engine.start("pokeapi")).willReturn(run);

        mockMvc.perform(post("/api/ingest/pokeapi"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.source").value("pokeapi"))
                .andExpect(jsonPath("$.status").value("RUNNING"));

        verify(ingestionExecutor).execute(any());
    }

    @Test
    void listRunsReturnsRunsFromRepositoryNewestFirst() throws Exception {
        IngestionRun run = runWithId(1L, "pokeapi", IngestionRunStatus.SUCCESS);
        given(runRepository.findTop50ByOrderByStartedAtDesc()).willReturn(List.of(run));

        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].source").value("pokeapi"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    void listSourcesReturnsSortedNames() throws Exception {
        given(sourceRegistry.names()).willReturn(Set.of("pokeapi", "github"));

        mockMvc.perform(get("/api/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("github"))
                .andExpect(jsonPath("$[1]").value("pokeapi"));
    }

    @Test
    void getRunReturnsMatchingRun() throws Exception {
        IngestionRun run = runWithId(7L, "pokeapi", IngestionRunStatus.FAILED);
        given(runRepository.findById(7L)).willReturn(Optional.of(run));

        mockMvc.perform(get("/api/runs/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void getRunReturns404WhenMissing() throws Exception {
        given(runRepository.findById(404L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/runs/404")).andExpect(status().isNotFound());
    }

    @Test
    void getRunReturns400ForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/runs/not-a-number")).andExpect(status().isBadRequest());
    }

    private static IngestionRun runWithId(long id, String source, IngestionRunStatus status) {
        IngestionRun run = new IngestionRun(source, status, Instant.now());
        ReflectionTestUtils.setField(run, "id", id);
        return run;
    }
}
