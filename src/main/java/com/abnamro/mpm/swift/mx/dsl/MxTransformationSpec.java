package com.abnamro.mpm.swift.mx.dsl;

import com.abnamro.mpm.swift.mt.dsl.Generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Root object representing a complete MX message transformation specification.
 * Maps to the YAML DSL structure for ISO 20022 XML messages.
 *
 * @param name            Transformation name
 * @param description     Transformation description
 * @param version         Transformation version
 * @param sourceFormat    Source message format (e.g., "pacs.008.001.08")
 * @param targetFormat    Target message format (e.g., "pacs.002.001.10")
 * @param generators      List of value generators (reuses MT generator DSL)
 * @param variables       List of variable extractors using XPath
 * @param transformations List of XPath-based transformation actions
 */
public record MxTransformationSpec(
        String name,
        String description,
        String version,
        String sourceFormat,
        String targetFormat,
        List<Generator> generators,
        List<MxVariable> variables,
        List<MxTransformation> transformations
) {
    public MxTransformationSpec {
        generators = generators != null ? new ArrayList<>(generators) : new ArrayList<>();
        variables = variables != null ? new ArrayList<>(variables) : new ArrayList<>();
        transformations = transformations != null ? new ArrayList<>(transformations) : new ArrayList<>();
    }
}
