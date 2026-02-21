package com.abnamro.mpm.swift.mt.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * Root object representing a complete MT message transformation specification.
 * Maps to the YAML DSL structure.
 *
 * @param name            Transformation name
 * @param description     Transformation description
 * @param version         Transformation version
 * @param sourceFormat    Source message format (e.g., "MT541")
 * @param targetFormat    Target message format (e.g., "MT545")
 * @param generators      List of value generators
 * @param variables       List of variable extractors
 * @param transformations List of transformation actions
 */
public record TransformationSpec(
        String name,
        String description,
        String version,
        String sourceFormat,
        String targetFormat,
        List<Generator> generators,
        List<Variable> variables,
        List<Transformation> transformations
) {
    /**
     * Canonical constructor with defensive copying and null-safe initialization.
     */
    public TransformationSpec {
        generators = generators != null ? new ArrayList<>(generators) : new ArrayList<>();
        variables = variables != null ? new ArrayList<>(variables) : new ArrayList<>();
        transformations = transformations != null ? new ArrayList<>(transformations) : new ArrayList<>();
    }
}

