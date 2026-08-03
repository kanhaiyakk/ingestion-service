package com.intentwise.ingestion.auth;

import com.intentwise.ingestion.config.AuthConfig;
import com.intentwise.ingestion.http.PageRequest;

/** Applies a source's authentication scheme to an outgoing page request. */
public interface AuthStrategy {

    String type();

    PageRequest apply(PageRequest request, AuthConfig cfg);
}
