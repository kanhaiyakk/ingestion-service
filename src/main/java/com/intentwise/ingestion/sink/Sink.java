package com.intentwise.ingestion.sink;

/** Persists extracted records to a destination. */
public interface Sink {

    String type();

    void write(WriteContext ctx);
}
