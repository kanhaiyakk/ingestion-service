package com.intentwise.ingestion.auth;

import com.intentwise.ingestion.config.AuthConfig;
import com.intentwise.ingestion.http.PageRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Adds an {@code Authorization: <scheme> <token>} header, token read from an env var. */
@Component
public class BearerAuth implements AuthStrategy {

    private static final String DEFAULT_SCHEME = "Bearer";

    private final EnvAccessor env;

    public BearerAuth(EnvAccessor env) {
        this.env = env;
    }

    @Override
    public String type() {
        return "bearer";
    }

    @Override
    public PageRequest apply(PageRequest request, AuthConfig cfg) {
        String token = Secrets.required(env, cfg.secretEnv());
        String scheme = (cfg.scheme() == null || cfg.scheme().isBlank()) ? DEFAULT_SCHEME : cfg.scheme();

        Map<String, String> headers = new LinkedHashMap<>(request.headers() != null ? request.headers() : Map.of());
        headers.put("Authorization", scheme + " " + token);
        return new PageRequest(request.url(), request.query(), headers);
    }
}
