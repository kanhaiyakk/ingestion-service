package com.intentwise.ingestion.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reads every {@code *.yaml}/{@code *.yml} file in the configured sources
 * directory at startup, deserializes it into a {@link SourceConfig}, validates
 * it, and registers it in the {@link SourceRegistry}. Fails fast: the first
 * unparsable or invalid file, or the first duplicate source name, aborts
 * loading with a message identifying the offending file.
 */
@Component
public class SourceConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(SourceConfigLoader.class);
    private static final Set<String> YAML_EXTENSIONS = Set.of("yaml", "yml");

    private final SourceRegistry registry;
    private final Validator validator;
    private final Path sourcesDir;
    private final YAMLMapper yamlMapper = YAMLMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    public SourceConfigLoader(SourceRegistry registry, Validator validator,
            @Value("${ingestion.sources-dir}") String sourcesDir) {
        this.registry = registry;
        this.validator = validator;
        this.sourcesDir = Path.of(sourcesDir);
    }

    @PostConstruct
    void loadOnStartup() {
        load(sourcesDir);
    }

    /**
     * Loads and registers every source config file found directly under
     * {@code dir}. Exposed with an explicit directory, rather than only via
     * the startup hook, so it can be exercised directly in tests.
     */
    public void load(Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new SourceConfigException("Sources directory does not exist: " + dir);
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream.filter(this::isYamlFile).sorted().toList();
        } catch (IOException e) {
            throw new SourceConfigException("Failed to list sources directory: " + dir, e);
        }

        for (Path file : files) {
            SourceConfig config = readAndValidate(file);
            try {
                registry.register(config);
            } catch (SourceConfigException e) {
                throw new SourceConfigException("Failed to register source from " + file + ": " + e.getMessage(), e);
            }
            log.info("Registered source '{}' from {}", config.name(), file.getFileName());
        }

        log.info("Loaded {} source config(s) from {}", files.size(), dir);
    }

    private boolean isYamlFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return YAML_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase());
    }

    private SourceConfig readAndValidate(Path file) {
        SourceConfig config;
        try {
            config = yamlMapper.readValue(file.toFile(), SourceConfig.class);
        } catch (IOException e) {
            throw new SourceConfigException("Failed to parse source config file " + file + ": " + e.getMessage(), e);
        }

        Set<ConstraintViolation<SourceConfig>> violations = validator.validate(config);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new SourceConfigException("Invalid source config in " + file + ": " + details);
        }

        return config;
    }
}
