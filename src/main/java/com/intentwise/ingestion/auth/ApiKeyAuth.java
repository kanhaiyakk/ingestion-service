package com.intentwise.ingestion.auth;

import com.intentwise.ingestion.config.AuthConfig;
import com.intentwise.ingestion.http.PageRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Injects a static API key, read from an env var, as a header or query parameter. */
@Component
public class ApiKeyAuth implements AuthStrategy {

    private final EnvAccessor env;

    public ApiKeyAuth(EnvAccessor env) {
        this.env = env;
    }

    @Override
    public String type() {
        return "api_key";
    }

    @Override
    public PageRequest apply(PageRequest request, AuthConfig cfg) {
        String secret = Secrets.required(env, cfg.secretEnv());

        if (cfg.headerName() != null && !cfg.headerName().isBlank()) {
            Map<String, String> headers = new LinkedHashMap<>(orEmpty(request.headers()));
            headers.put(cfg.headerName(), secret);
            return new PageRequest(request.url(), request.query(), headers);
        }
        if (cfg.queryParam() != null && !cfg.queryParam().isBlank()) {
            Map<String, String> query = new LinkedHashMap<>(orEmpty(request.query()));
            query.put(cfg.queryParam(), secret);
            return new PageRequest(request.url(), query, request.headers());
        }
        throw new AuthConfigException("auth type 'api_key' requires either auth.headerName or auth.queryParam to be set");
    }

    private static Map<String, String> orEmpty(Map<String, String> map) {
        return map != null ? map : Map.of();
    }
}
