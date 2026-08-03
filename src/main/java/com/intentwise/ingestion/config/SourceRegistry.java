package com.intentwise.ingestion.config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory registry of loaded {@link SourceConfig}s, keyed by source name.
 * Populated once at startup by {@link SourceConfigLoader}.
 */
@Component
public class SourceRegistry {

    private final Map<String, SourceConfig> sourcesByName = new ConcurrentHashMap<>();

    /**
     * Registers a source config. Throws if a source with the same name is
     * already registered.
     */
    public void register(SourceConfig config) {
        SourceConfig existing = sourcesByName.putIfAbsent(config.name(), config);
        if (existing != null) {
            throw new SourceConfigException("Duplicate source name: '" + config.name() + "'");
        }
    }

    public SourceConfig get(String name) {
        SourceConfig config = sourcesByName.get(name);
        if (config == null) {
            throw new SourceConfigException("No source registered with name '" + name + "'");
        }
        return config;
    }

    public Set<String> names() {
        return Set.copyOf(sourcesByName.keySet());
    }
}
