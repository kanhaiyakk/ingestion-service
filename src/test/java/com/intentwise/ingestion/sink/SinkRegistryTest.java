package com.intentwise.ingestion.sink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SinkRegistry}: lookup by type, unknown type fails clearly. */
class SinkRegistryTest {

    private final SinkRegistry registry = new SinkRegistry(List.of(new StdoutSink()));

    @Test
    void getReturnsStdoutSink() {
        assertThat(registry.get("stdout")).isInstanceOf(StdoutSink.class);
    }

    @Test
    void getThrowsClearExceptionForUnknownType() {
        assertThatThrownBy(() -> registry.get("s3"))
                .isInstanceOf(SinkConfigException.class)
                .hasMessageContaining("s3");
    }
}
