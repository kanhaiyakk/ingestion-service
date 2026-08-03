# Generic Data Ingestion Service

A configuration-driven service that ingests data from arbitrary HTTP APIs and
persists it to a pluggable destination. Adding a new data source means adding a
YAML file, not writing code.

## Status

Scaffold in place. Ingestion engine, strategies, and demo sources are added in
subsequent commits (see the git history).

## Quickstart

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080`, backed by a Postgres
instance started by Compose. Health check:

```bash
curl http://localhost:8080/actuator/health
```

## Architecture

_A design note and diagram will be added here._ The core idea: a source is
described by declarative config (auth, pagination, record location, sink). A
generic engine reads that config, selects the matching pluggable strategies, and
runs a uniform `fetch -> extract -> write` loop. New sources and new
destinations are config/plugin changes, never rewrites.

## Public APIs used

_To be documented (PokeAPI, GitHub, Open Brewery DB)._

## Design decisions & tradeoffs

_To be documented._

## What I would do with more time

_To be documented._

## Use of AI tools

Built with Claude Code, working through the design ticket by ticket.

**Where it got something wrong, and how I caught it:** implementing the HTTP
layer's WireMock tests, the assistant first added `org.wiremock:wiremock` (the
current "core" artifact) as the test dependency. Running `mvn test` (not just
compiling) surfaced two real problems in sequence:

1. `NoSuchMethodError: RequestConfig$Builder.setProtocolUpgradeEnabled` —
   Spring Boot's dependency-management BOM silently downgrades
   `httpclient5`/`httpcore5` to the 5.2.x line, which predates a method
   WireMock 3.13.2's compiled code calls. Nothing in `pom.xml` hinted at
   this; `mvn dependency:tree -Dverbose=true` was needed to see the BOM
   overriding WireMock's own declared version.
2. After pinning `httpclient5`/`httpcore5` explicitly to silence that,
   startup failed differently: `FatalStartupException: Jetty 11 is not
   present`. The `wiremock` core artifact no longer bundles an embedded
   server as of the 3.x split — that now lives in a separate
   `wiremock-jetty12` extension.

Rather than keep chasing version pins, the fix was to swap to
`org.wiremock:wiremock-standalone`, a self-contained artifact bundling a
matched Jetty and HttpClient5, which needs no manual pinning at all. Caught
by actually executing the test suite and reading the runtime stack traces
closely enough to tell a classpath *version conflict* apart from a *missing
module* — a compile-only check would have missed both.

_More to document as later tickets land: architecture rationale, design
tradeoffs, what I'd do with more time._
