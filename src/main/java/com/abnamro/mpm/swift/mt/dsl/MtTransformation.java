package com.abnamro.mpm.swift.mt.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single transformation action on a specific block.
 *
 * Supported actions:
 * - set: Set field values
 * - delete: Delete fields
 * - deleteSequence: Delete entire sequences
 * - insertAfter: Insert sequences after a field
 * - insertBefore: Insert sequences before a field
 *
 * @param block     Block identifier: 1, 2, 3, 4, 5, or S (system block)
 * @param sequence  For block 4: GENL, TRADDET, FIAC, etc.
 * @param action    Transformation action type
 * @param field     Field reference for insertAfter/insertBefore
 * @param fields    List of fields to transform
 * @param sequences List of sequences to insert
 */
public record MtTransformation(
        String block,
        String sequence,
        ActionType action,
        String field,
        List<FieldConfig> fields,
        List<SequenceConfig> sequences
) {
    /**
     * Canonical constructor with defensive copying and null-safe initialization.
     */
    public MtTransformation {
        fields = fields != null ? new ArrayList<>(fields) : new ArrayList<>();
        sequences = sequences != null ? new ArrayList<>(sequences) : new ArrayList<>();
    }

    /**
     * Convenience constructor for Jackson deserialization with default empty lists.
     */
    public MtTransformation(String block, String sequence, ActionType action, String field) {
        this(block, sequence, action, field, new ArrayList<>(), new ArrayList<>());
    }
}

