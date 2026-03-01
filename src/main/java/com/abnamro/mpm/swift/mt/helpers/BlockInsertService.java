package com.abnamro.mpm.swift.mt.helpers;

import com.abnamro.mpm.swift.common.helpers.FieldResolver;
import com.abnamro.mpm.swift.mt.dsl.*;
import com.prowidesoftware.swift.model.*;
import com.prowidesoftware.swift.model.field.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.abnamro.mpm.swift.mt.SwiftConstants.*;

/**
 * Service for inserting sequences into SWIFT messages.
 */
public class BlockInsertService {

    private final FieldResolver fieldResolver;

    public BlockInsertService(FieldResolver fieldResolver) {
        this.fieldResolver = fieldResolver;
    }

    /**
     * Execute INSERT AFTER action - insert sequences after a field.
     */
    public void executeInsertAfter(MtTransformation mtTransformation, SwiftMessage message, Map<String, String> context) {
        String block = mtTransformation.block();

        if (!BLOCK_4.equals(block)) {
            throw new UnsupportedOperationException("InsertAfter action only supported for block 4");
        }

        SwiftBlock4 block4 = message.getBlock4();
        if (block4 == null) return;

        String afterField = mtTransformation.field();
        String targetSequence = mtTransformation.sequence();
        int insertPosition = findFieldPosition(block4, afterField, targetSequence);

        if (insertPosition < 0) {
            throw new IllegalStateException("Field not found: " + afterField +
                (targetSequence != null ? " in sequence " + targetSequence : ""));
        }

        // Insert after the found position
        insertPosition++;

        // Build tags to insert
        List<Tag> tagsToInsert = buildTagsToInsert(mtTransformation, context);

        // Insert all tags at the calculated position
        block4.getTags().addAll(insertPosition, tagsToInsert);
    }

    /**
     * Execute INSERT BEFORE action - insert sequences before a field.
     */
    public void executeInsertBefore(MtTransformation mtTransformation, SwiftMessage message, Map<String, String> context) {
        String block = mtTransformation.block();

        if (!BLOCK_4.equals(block)) {
            throw new UnsupportedOperationException("InsertBefore action only supported for block 4");
        }

        SwiftBlock4 block4 = message.getBlock4();
        if (block4 == null) return;

        String beforeField = mtTransformation.field();
        String targetSequence = mtTransformation.sequence();
        int insertPosition = findFieldPosition(block4, beforeField, targetSequence);

        if (insertPosition < 0) {
            throw new IllegalStateException("Field not found: " + beforeField +
                (targetSequence != null ? " in sequence " + targetSequence : ""));
        }

        // Build tags to insert
        List<Tag> tagsToInsert = buildTagsToInsert(mtTransformation, context);

        // Insert all tags at the calculated position
        block4.getTags().addAll(insertPosition, tagsToInsert);
    }

    /**
     * Build the list of tags to insert for sequences and/or fields.
     */
    private List<Tag> buildTagsToInsert(MtTransformation mtTransformation, Map<String, String> context) {
        List<Tag> tagsToInsert = new ArrayList<>();

        // Insert sequences (with 16R/16S markers)
        for (SequenceConfig seqConfig : mtTransformation.sequences()) {
            // Add sequence start marker
            tagsToInsert.add(new Tag(SEQUENCE_START_MARKER, seqConfig.sequence()));

            // Add fields within the sequence
            for (FieldConfig fieldConfig : seqConfig.fields()) {
                addFieldTag(tagsToInsert, fieldConfig, context);
            }

            // Add sequence end marker
            tagsToInsert.add(new Tag(SEQUENCE_END_MARKER, seqConfig.sequence()));
        }

        // Insert simple fields (not part of a sequence)
        for (FieldConfig fieldConfig : mtTransformation.fields()) {
            addFieldTag(tagsToInsert, fieldConfig, context);
        }

        return tagsToInsert;
    }

    /**
     * Helper method to add a field tag to the list.
     */
    private void addFieldTag(List<Tag> tagList, FieldConfig fieldConfig, Map<String, String> context) {
        String fieldSpec = fieldConfig.field();
        String value = fieldResolver.resolveValue(fieldConfig, context);

        String tag;
        String qualifier = null;

        if (fieldSpec.contains(QUALIFIER_SEPARATOR)) {
            String[] parts = fieldSpec.split(QUALIFIER_SEPARATOR);
            tag = parts[0];
            qualifier = parts[1];
        } else {
            tag = fieldSpec;
        }

        String tagValue = qualifier != null ? ":" + qualifier + "//" + value : value;
        tagList.add(new Tag(tag, tagValue));
    }

    /**
     * Find the position of a field in block 4, optionally within a specific sequence.
     *
     * @param block4 The block to search in
     * @param fieldSpec The field specification (e.g., "23G" or "20C::SEME")
     * @param targetSequence Optional sequence name to limit search within (e.g., "GENL")
     * @return The position index of the field, or -1 if not found
     */
    private int findFieldPosition(SwiftBlock4 block4, String fieldSpec, String targetSequence) {
        String tag;
        String qualifier = null;

        if (fieldSpec.contains(QUALIFIER_SEPARATOR)) {
            String[] parts = fieldSpec.split(QUALIFIER_SEPARATOR);
            tag = parts[0];
            qualifier = parts[1];
        } else {
            tag = fieldSpec;
        }

        List<Tag> tags = block4.getTags();
        String currentSequence = null;

        for (int i = 0; i < tags.size(); i++) {
            Tag existingTag = tags.get(i);

            // Track current sequence
            if (existingTag.getName().equals(SEQUENCE_START_MARKER)) {
                currentSequence = existingTag.getValue();
            } else if (existingTag.getName().equals(SEQUENCE_END_MARKER)) {
                currentSequence = null;
            }

            if (existingTag.getName().equals(tag)) {
                // If a target sequence is specified, only match within that sequence
                if (targetSequence != null && !targetSequence.equals(currentSequence)) {
                    continue; // Skip this tag, it's not in the target sequence
                }

                if (qualifier != null) {
                    Field field = Field.getField(existingTag);
                    String fieldQualifier = field.getComponent(1);
                    if (qualifier.equals(fieldQualifier)) {
                        return i;
                    }
                } else {
                    return i;
                }
            }
        }
        return -1;
    }
}

