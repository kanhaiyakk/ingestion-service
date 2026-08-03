package com.intentwise.ingestion.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.intentwise.ingestion.config.AuthConfig;
import com.intentwise.ingestion.http.PageRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link NoAuth}: type, and that apply is a true no-op. */
class NoAuthTest {

    private final NoAuth auth = new NoAuth();

    @Test
    void typeIsNone() {
        assertThat(auth.type()).isEqualTo("none");
    }

    @Test
    void applyReturnsSameRequestInstance() {
        PageRequest request = new PageRequest("https://example.com", Map.of(), Map.of());

        PageRequest result = auth.apply(request, new AuthConfig("none", null, null, null, null));

        assertThat(result).isSameAs(request);
    }
}
