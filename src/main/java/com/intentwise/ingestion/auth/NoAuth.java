package com.intentwise.ingestion.auth;

import com.intentwise.ingestion.config.AuthConfig;
import com.intentwise.ingestion.http.PageRequest;
import org.springframework.stereotype.Component;

/** No-op auth strategy for public sources that need no credentials. */
@Component
public class NoAuth implements AuthStrategy {

    @Override
    public String type() {
        return AuthConfig.NONE;
    }

    @Override
    public PageRequest apply(PageRequest request, AuthConfig cfg) {
        return request;
    }
}
