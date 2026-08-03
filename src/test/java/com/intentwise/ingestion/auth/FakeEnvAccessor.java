package com.intentwise.ingestion.auth;

import java.util.Map;

/** Map-backed {@link EnvAccessor} so tests don't depend on real process environment variables. */
class FakeEnvAccessor implements EnvAccessor {

    private final Map<String, String> values;

    FakeEnvAccessor(Map<String, String> values) {
        this.values = values;
    }

    @Override
    public String get(String name) {
        return values.get(name);
    }
}
