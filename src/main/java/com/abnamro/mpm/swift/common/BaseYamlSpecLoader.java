package com.abnamro.mpm.swift.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for loading YAML specification files.
 * Provides common functionality for parsing YAML with Jackson.
 *
 * @param <T> The type of specification to load
 */
public abstract class BaseYamlSpecLoader<T> {
    protected final ObjectMapper mapper;
    private final Class<T> specClass;

    protected BaseYamlSpecLoader(Class<T> specClass) {
        this.specClass = specClass;
        this.mapper = new ObjectMapper(new YAMLFactory());
        // Enable case-insensitive enum deserialization
        this.mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        // Ignore unknown properties for forward compatibility
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Allow subclasses to configure the mapper
        configureMapper(mapper);
    }

    /**
     * Hook for subclasses to configure the ObjectMapper.
     * Override this method to add custom configuration.
     *
     * @param mapper The ObjectMapper to configure
     */
    protected void configureMapper(ObjectMapper mapper) {
        // Default: no additional configuration
    }

    /**
     * Load a specification from a YAML string.
     *
     * @param yamlContent The YAML content as a string
     * @return The parsed specification
     */
    public T loadFromString(String yamlContent) {
        try {
            return mapper.readValue(yamlContent, specClass);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML content", e);
        }
    }

    /**
     * Load a specification from an InputStream.
     *
     * @param inputStream The input stream containing YAML
     * @return The parsed specification
     */
    public T loadFromStream(InputStream inputStream) {
        try {
            return mapper.readValue(inputStream, specClass);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML stream", e);
        }
    }

    /**
     * Load a specification from a Reader.
     *
     * @param reader The reader containing YAML
     * @return The parsed specification
     */
    public T loadFromReader(Reader reader) {
        try {
            return mapper.readValue(reader, specClass);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML from reader", e);
        }
    }

    /**
     * Load a specification from a file path.
     *
     * @param path The path to the YAML file
     * @return The parsed specification
     * @throws IOException If an I/O error occurs
     */
    public T loadFromFile(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return loadFromStream(is);
        }
    }

    /**
     * Load a specification from a classpath resource.
     *
     * @param resourcePath The classpath resource path
     * @return The parsed specification
     */
    public T loadFromResource(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        return loadFromStream(is);
    }
}

