package com.abnamro.mpm.swift.mx;

import com.abnamro.mpm.swift.mx.dsl.MxTransformationSpec;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads MxTransformationSpec from YAML files.
 */
public class MxSpecLoader {

    private final ObjectMapper mapper;

    public MxSpecLoader() {
        this.mapper = new ObjectMapper(new YAMLFactory());
        this.mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public MxTransformationSpec loadFromFile(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return mapper.readValue(is, MxTransformationSpec.class);
        }
    }

    public MxTransformationSpec loadFromResource(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        try {
            return mapper.readValue(is, MxTransformationSpec.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML resource: " + resourcePath, e);
        }
    }

    public MxTransformationSpec loadFromString(String yaml) {
        try {
            return mapper.readValue(yaml, MxTransformationSpec.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML content", e);
        }
    }
}
