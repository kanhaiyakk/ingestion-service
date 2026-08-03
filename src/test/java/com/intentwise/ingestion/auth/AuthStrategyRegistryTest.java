package com.intentwise.ingestion.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AuthStrategyRegistry}: lookup by type, unknown type fails clearly. */
class AuthStrategyRegistryTest {

    private final AuthStrategyRegistry registry = new AuthStrategyRegistry(List.of(new NoAuth()));

    @Test
    void getReturnsStrategyMatchingType() {
        assertThat(registry.get("none")).isInstanceOf(NoAuth.class);
    }

    @Test
    void getThrowsClearExceptionForUnknownType() {
        assertThatThrownBy(() -> registry.get("oauth2"))
                .isInstanceOf(AuthConfigException.class)
                .hasMessageContaining("oauth2");
    }
}
