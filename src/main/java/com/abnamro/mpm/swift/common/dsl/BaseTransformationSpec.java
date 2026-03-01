package com.abnamro.mpm.swift.common.dsl;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for transformation specifications.
 * Contains common properties shared by MT and MX transformation specs.
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public abstract class BaseTransformationSpec {
    private String name;
    private TransformationType type;
    private String description;
    private String version;
    private String sourceFormat;
    private String targetFormat;
    private List<Generator> generators = new ArrayList<>();
}

