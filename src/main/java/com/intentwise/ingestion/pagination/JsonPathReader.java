package com.intentwise.ingestion.pagination;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Evaluates a JSONPath expression against a Jackson {@link JsonNode}; absent/malformed paths yield null, never throw. */
final class JsonPathReader {

    private static final Logger log = LoggerFactory.getLogger(JsonPathReader.class);

    private final Configuration configuration;

    JsonPathReader(ObjectMapper objectMapper) {
        this.configuration = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider(objectMapper))
                .mappingProvider(new JacksonMappingProvider(objectMapper))
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();
    }

    JsonNode read(JsonNode node, String path) {
        try {
            return JsonPath.using(configuration).parse(node).read(path, JsonNode.class);
        } catch (RuntimeException e) {
            log.warn("Failed to evaluate JSONPath '{}': {}", path, e.getMessage());
            return null;
        }
    }
}
