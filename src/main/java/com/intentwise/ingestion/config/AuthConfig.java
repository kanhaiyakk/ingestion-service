package com.intentwise.ingestion.config;

/**
 * Describes how requests to a source are authenticated. {@code type} selects
 * the {@code AuthStrategy} implementation to use and defaults to
 * {@code "none"} when omitted; the remaining fields are interpreted by that
 * strategy (e.g. header name/scheme for bearer tokens, query param name for
 * API keys). {@code secretEnv} names the environment variable holding the
 * actual secret value — secrets are never written directly in YAML.
 */
public record AuthConfig(String type, String headerName, String queryParam, String scheme, String secretEnv) {

    public static final String NONE = "none";

    public AuthConfig {
        if (type == null || type.isBlank()) {
            type = NONE;
        }
    }
}
