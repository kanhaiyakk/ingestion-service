package com.intentwise.ingestion.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.intentwise.ingestion.config.AuthConfig;
import com.intentwise.ingestion.http.PageRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link BearerAuth}: default/custom scheme, and missing secret. */
class BearerAuthTest {

    private final EnvAccessor env = new FakeEnvAccessor(Map.of("GITHUB_TOKEN", "ghp_abc123"));
    private final BearerAuth auth = new BearerAuth(env);

    @Test
    void addsAuthorizationHeaderWithDefaultBearerScheme() {
        PageRequest request = new PageRequest("https://example.com", Map.of(), Map.of());
        AuthConfig cfg = new AuthConfig("bearer", null, null, null, "GITHUB_TOKEN");

        PageRequest result = auth.apply(request, cfg);

        assertThat(result.headers()).containsEntry("Authorization", "Bearer ghp_abc123");
    }

    @Test
    void usesConfiguredSchemeWhenProvided() {
        PageRequest request = new PageRequest("https://example.com", Map.of(), Map.of());
        AuthConfig cfg = new AuthConfig("bearer", null, null, "Token", "GITHUB_TOKEN");

        PageRequest result = auth.apply(request, cfg);

        assertThat(result.headers()).containsEntry("Authorization", "Token ghp_abc123");
    }

    @Test
    void throwsNamingTheVariableWhenEnvVarUnset() {
        PageRequest request = new PageRequest("https://example.com", Map.of(), Map.of());
        AuthConfig cfg = new AuthConfig("bearer", null, null, null, "MISSING_TOKEN");

        assertThatThrownBy(() -> auth.apply(request, cfg))
                .isInstanceOf(AuthConfigException.class)
                .hasMessageContaining("MISSING_TOKEN");
    }
}
