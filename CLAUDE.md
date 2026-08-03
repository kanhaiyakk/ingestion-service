# Project: Generic Data Ingestion Service

A configuration-driven service that ingests data from arbitrary HTTP APIs and
persists it to a pluggable destination. **Adding a new data source is a YAML
change; adding a new auth scheme, pagination style, or destination is one new
class.** No source-specific logic ever lives in the engine.

## Stack & conventions

- Java 17, Spring Boot 3.2.x, Maven.
- Base package: `com.intentwise.ingestion`.
- Constructor injection only — never field injection. No Lombok (keep the code
  explainable by hand).
- Use Java `record`s for all config and DTO types.
- Every public class/interface gets a short Javadoc explaining its role.
- Logging via SLF4J (`private static final Logger log = ...`). Log at INFO for
  run lifecycle, DEBUG for per-page detail, WARN/ERROR for retries and failures.
- Secrets are **never** written in YAML. Config references an environment
  variable name; the value is resolved from the environment at runtime.

## Package layout

```
com.intentwise.ingestion
├── config      // SourceConfig + sub-records + YAML loader/registry
├── http        // HttpFetcher (RestClient + Resilience4j)
├── auth        // AuthStrategy interface + implementations
├── pagination  // Paginator interface + implementations
├── extract     // RecordExtractor
├── sink        // Sink interface + implementations
├── engine      // IngestionEngine orchestrator
├── run         // IngestionRun entity + repository + status
├── persistence // RawRecord entity + repository
└── api         // REST controllers + DTOs
```

## Strategy-registry pattern (use this everywhere)

Each pluggable family (auth, pagination, sink) is an interface with a
`String type()` method. Every implementation is a `@Component`. A small registry
component injects `List<T>` and indexes them into a `Map<String, T>` by
`type()`, exposing `T get(String type)` that throws a clear exception on unknown
types. **Consequence: adding a strategy = adding a `@Component`; no existing
code is edited.** State this property in the README.

## Core contracts (keep signatures stable across tickets)

```java
// config
record SourceConfig(String name, String baseUrl, String path,
                    RequestConfig request, AuthConfig auth,
                    PaginationConfig pagination, ExtractConfig extract,
                    SinkConfig sink, RateLimitConfig rateLimit) {}

record RequestConfig(Map<String,String> queryParams, Map<String,String> headers) {}
record AuthConfig(String type, String headerName, String queryParam,
                  String scheme, String secretEnv) {}
record PaginationConfig(String type, String pageParam, String sizeParam,
                        Integer pageSize, Integer startPage, String cursorPath,
                        Integer maxPages) {}
record ExtractConfig(String recordsPath, String idPath) {}
record SinkConfig(String type, String table, Map<String,String> options) {}
record RateLimitConfig(Double requestsPerSecond) {}

// http
record PageRequest(String url, Map<String,String> query, Map<String,String> headers) {}
record FetchResponse(int status, Map<String,String> headers, JsonNode body, String rawBody) {}
interface HttpFetcher { FetchResponse fetch(PageRequest request); }

// auth
interface AuthStrategy { String type(); PageRequest apply(PageRequest request, AuthConfig cfg); }

// pagination — engine loops: call next(ctx) -> fetch -> record response into ctx -> repeat until empty
interface Paginator {
    String type();
    Optional<PageRequest> next(PaginationContext ctx); // empty => done
}
// PaginationContext holds SourceConfig, the last FetchResponse (null on first call),
// and pagesFetched. maxPages is a hard safety cap.

// extract
record ExtractedRecord(String key, JsonNode payload) {}
interface RecordExtractor { List<ExtractedRecord> extract(FetchResponse response, ExtractConfig cfg); }

// sink
record WriteContext(String sourceName, long runId, List<ExtractedRecord> records) {}
interface Sink { String type(); void write(WriteContext ctx); }

// engine
interface IngestionEngine { IngestionRun run(String sourceName); }
```

## Persistence

- `RawRecord`: `id`, `source`, `recordKey`, `payload` (JSONB), `runId`,
  `ingestedAt`. Unique constraint on `(source, recordKey)`; writes are idempotent
  upserts (insert-or-update on conflict) so re-runs don't duplicate.
- Map JSONB with Hibernate 6: `@JdbcTypeCode(SqlTypes.JSON)` on a `String` field
  holding the serialized JSON.
- `IngestionRun`: `id`, `source`, `status` (RUNNING/SUCCESS/FAILED),
  `recordsWritten`, `pagesFetched`, `startedAt`, `finishedAt`, `errorMessage`.

## Testing conventions

- JUnit 5 + AssertJ.
- Unit-test every auth and pagination strategy in isolation.
- HTTP-touching tests use **WireMock** to stand up a fake API (control status
  codes, headers, pagination shape, 429s).
- Persistence and end-to-end tests use **Testcontainers** Postgres — never H2,
  because JSONB and upsert-on-conflict are Postgres-specific.
- Add each new dependency (Resilience4j, WireMock, Testcontainers, springdoc) in
  the ticket that first uses it, not up front.
