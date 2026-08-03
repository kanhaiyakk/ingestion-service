package com.intentwise.ingestion.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.intentwise.ingestion.config.AuthConfig;
import com.intentwise.ingestion.http.PageRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ApiKeyAuth}: header vs query injection, and failure modes. */
class ApiKeyAuthTest {

    private final EnvAccessor env = new FakeEnvAccessor(Map.of("POKEAPI_KEY", "secret-123"));
    private final ApiKeyAuth auth = new ApiKeyAuth(env);

    @Test
    void injectsSecretAsHeaderWhenHeaderNameConfigured() {
        PageRequest request = new PageRequest("https://example.com", Map.of(), Map.of());
        AuthConfig cfg = new AuthConfig("api_key", "X-Api-Key", null, null, "POKEAPI_KEY");

        PageRequest result = auth.apply(request, cfg);

        assertThat(result.headers()).containsEntry("X-Api-Key", "secret-123");
        assertThat(result.query()).isEmpty();
    }

    @Test
    void injectsSecretAsQueryParamWhenQueryParamConfigured() {
        PageRequest request = new PageRequest("https://example.com", Map.of(), Map.of());
        AuthConfig cfg = new AuthConfig("api_key", null, "api_key", null, "POKEAPI_KEY");

        PageRequest result = auth.apply(request, cfg);

        assertThat(result.query()).containsEntry("api_key", "secret-123");
        assertThat(result.headers()).isEmpty();
    }

    @Test
    void headerWinsWhenBothConfigured() {
        PageRequest request = new PageRequest("https://example.com", Map.of(), Map.of());
        AuthConfig cfg = new AuthConfig("api_key", "X-Api-Key", "api_key", null, "POKEAPI_KEY");

        PageRequest result = auth.apply(request, cfg);

        assertThat(result.headers()).containsEntry("X-Api-Key", "secret-123");
        assertThat(result.query()).isEmpty();
    }

    @Test
    void preservesExistingHeadersAndQueryParams() {
        PageRequest request = new PageRequest("https://example.com", Map.of("page", "2"), Map.of("Accept", "application/json"));
        AuthConfig cfg = new AuthConfig("api_key", "X-Api-Key", null, null, "POKEAPI_KEY");

        PageRequest result = auth.apply(request, cfg);

        assertThat(result.headers()).containsEntry("Accept", "application/json").containsEntry("X-Api-Key", "secret-123");
        assertThat(result.query()).containsEntry("page", "2");
    }

    @Test
    void throwsWhenNeitherHeaderNorQueryConfigured() {
        PageRequest request = new PageRequest("https://example.com", Map.of(), Map.of());
        AuthConfig cfg = new AuthConfig("api_key", null, null, null, "POKEAPI_KEY");

        assertThatThrownBy(() -> auth.apply(request, cfg)).isInstanceOf(AuthConfigException.class);
    }

    @Test
    void throwsNamingTheVariableWhenEnvVarUnset() {
        PageRequest request = new PageRequest("https://example.com", Map.of(), Map.of());
        AuthConfig cfg = new AuthConfig("api_key", "X-Api-Key", null, null, "MISSING_VAR");

        assertThatThrownBy(() -> auth.apply(request, cfg))
                .isInstanceOf(AuthConfigException.class)
                .hasMessageContaining("MISSING_VAR");
    }
}
