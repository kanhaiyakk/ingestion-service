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

_To be documented, including one place an AI tool got something wrong and how I
caught it._
