package com.abnamro.mpm.swift.mt;

import com.abnamro.mpm.swift.mt.dsl.TransformationSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for loading TransformationSpec from YAML files.
 * Uses Jackson for YAML parsing with case-insensitive enum handling.
 */
public class TransformationSpecLoader {

    private final ObjectMapper mapper;

    public TransformationSpecLoader() {
        this.mapper = new ObjectMapper(new YAMLFactory());
        // Enable case-insensitive enum deserialization
        this.mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        // Ignore unknown properties
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Load a TransformationSpec from a YAML string.
     */
    public TransformationSpec loadFromString(String yamlContent) {
        try {
            return mapper.readValue(yamlContent, TransformationSpec.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML content", e);
        }
    }

    /**
     * Load a TransformationSpec from an InputStream.
     */
    public TransformationSpec loadFromStream(InputStream inputStream) {
        try {
            return mapper.readValue(inputStream, TransformationSpec.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML stream", e);
        }
    }

    /**
     * Load a TransformationSpec from a Reader.
     */
    public TransformationSpec loadFromReader(Reader reader) {
        try {
            return mapper.readValue(reader, TransformationSpec.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML from reader", e);
        }
    }

    /**
     * Load a TransformationSpec from a file path.
     */
    public TransformationSpec loadFromFile(Path path) throws Exception {
        try (InputStream is = Files.newInputStream(path)) {
            return loadFromStream(is);
        }
    }

    /**
     * Load a TransformationSpec from a classpath resource.
     */
    public TransformationSpec loadFromResource(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        return loadFromStream(is);
    }
}

