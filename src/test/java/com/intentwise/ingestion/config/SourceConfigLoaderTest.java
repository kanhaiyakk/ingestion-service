package com.intentwise.ingestion.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SourceConfigLoader}. Exercises the loader directly
 * (no Spring context) against fixture directories under
 * {@code src/test/resources/config}.
 */
class SourceConfigLoaderTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private SourceRegistry registry;
    private SourceConfigLoader loader;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        registry = new SourceRegistry();
        loader = new SourceConfigLoader(registry, validator, "unused");
    }

    @Test
    void validFileLoadsAndRegisters() {
        loader.load(fixture("valid"));

        assertThat(registry.names()).containsExactly("pokeapi");

        SourceConfig config = registry.get("pokeapi");
        assertThat(config.baseUrl()).isEqualTo("https://pokeapi.co/api/v2");
        assertThat(config.path()).isEqualTo("/pokemon");
        assertThat(config.pagination().type()).isEqualTo("cursor");
        assertThat(config.pagination().cursorPath()).isEqualTo("$.next");
        assertThat(config.extract().recordsPath()).isEqualTo("$.results");
        assertThat(config.auth().type()).isEqualTo("none");
    }

    @Test
    void invalidFileMissingBaseUrlFailsValidation() {
        assertThatThrownBy(() -> loader.load(fixture("invalid-missing-base-url")))
                .isInstanceOf(SourceConfigException.class)
                .hasMessageContaining("broken.yaml")
                .hasMessageContaining("baseUrl");

        assertThat(registry.names()).isEmpty();
    }

    @Test
    void duplicateNamesAreRejected() {
        assertThatThrownBy(() -> loader.load(fixture("duplicate-names")))
                .isInstanceOf(SourceConfigException.class)
                .hasMessageContaining("dup-source");
    }

    private static Path fixture(String subDir) {
        return Path.of("src/test/resources/config", subDir);
    }
}
