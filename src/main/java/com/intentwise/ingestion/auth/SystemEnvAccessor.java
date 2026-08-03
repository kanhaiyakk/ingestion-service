package com.intentwise.ingestion.auth;

import org.springframework.stereotype.Component;

/** Default {@link EnvAccessor} backed by the real process environment. */
@Component
public class SystemEnvAccessor implements EnvAccessor {

    @Override
    public String get(String name) {
        return System.getenv(name);
    }
}
