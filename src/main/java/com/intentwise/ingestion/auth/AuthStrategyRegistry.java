package com.intentwise.ingestion.auth;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Indexes every {@link AuthStrategy} bean by {@link AuthStrategy#type()}. */
@Component
public class AuthStrategyRegistry {

    private final Map<String, AuthStrategy> strategiesByType;

    public AuthStrategyRegistry(List<AuthStrategy> strategies) {
        this.strategiesByType = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(AuthStrategy::type, Function.identity()));
    }

    public AuthStrategy get(String type) {
        AuthStrategy strategy = strategiesByType.get(type);
        if (strategy == null) {
            throw new AuthConfigException("Unknown auth type '" + type + "'; available: " + strategiesByType.keySet());
        }
        return strategy;
    }
}
