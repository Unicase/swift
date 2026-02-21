package com.abnamro.mpm.swift.mt.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for a sequence within a transformation.
 * Used for insertAfter/insertBefore actions to define new sequences to add.
 *
 * @param sequence The sequence name (e.g., "LINK", "FIA")
 * @param fields   List of fields within the sequence
 */
public record SequenceConfig(String sequence, List<FieldConfig> fields) {

    /**
     * Canonical constructor with defensive copying and null-safe initialization.
     */
    public SequenceConfig {
        fields = fields != null ? new ArrayList<>(fields) : new ArrayList<>();
    }

    /**
     * Convenience constructor for creating a sequence with no fields initially.
     */
    public SequenceConfig(String sequence) {
        this(sequence, new ArrayList<>());
    }
}

