package com.intentwise.ingestion.sink;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Indexes every {@link Sink} bean by {@link Sink#type()}. */
@Component
public class SinkRegistry {

    private final Map<String, Sink> sinksByType;

    public SinkRegistry(List<Sink> sinks) {
        this.sinksByType = sinks.stream().collect(Collectors.toUnmodifiableMap(Sink::type, Function.identity()));
    }

    public Sink get(String type) {
        Sink sink = sinksByType.get(type);
        if (sink == null) {
            throw new SinkConfigException("Unknown sink type '" + type + "'; available: " + sinksByType.keySet());
        }
        return sink;
    }
}
