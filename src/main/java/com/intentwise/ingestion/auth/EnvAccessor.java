package com.intentwise.ingestion.auth;

/** Resolves a named environment variable; a seam so tests don't depend on real process environment. */
public interface EnvAccessor {

    String get(String name);
}
