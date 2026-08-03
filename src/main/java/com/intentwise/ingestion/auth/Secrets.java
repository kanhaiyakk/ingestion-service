package com.intentwise.ingestion.auth;

/** Resolves a {@code secretEnv}-named value, failing clearly when it's unset. */
final class Secrets {

    private Secrets() {
    }

    static String required(EnvAccessor env, String secretEnv) {
        if (secretEnv == null || secretEnv.isBlank()) {
            throw new AuthConfigException("auth.secretEnv must be configured to resolve a secret");
        }
        String value = env.get(secretEnv);
        if (value == null || value.isBlank()) {
            throw new AuthConfigException("Environment variable '" + secretEnv + "' referenced by auth.secretEnv is not set");
        }
        return value;
    }
}
