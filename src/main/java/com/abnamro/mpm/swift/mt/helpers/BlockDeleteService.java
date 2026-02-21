package com.abnamro.mpm.swift.mt.helpers;

import com.abnamro.mpm.swift.mt.dsl.*;
import com.prowidesoftware.swift.model.*;
import com.prowidesoftware.swift.model.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.abnamro.mpm.swift.mt.SwiftConstants.*;

/**
 * Service for deleting fields and sequences from SWIFT messages.
 */
public class BlockDeleteService {

    private static final Logger log = LoggerFactory.getLogger(BlockDeleteService.class);

    /**
     * Execute DELETE action - remove fields.
     */
    public void executeDelete(Transformation transformation, SwiftMessage message) {
        String block = transformation.block();

        if (!BLOCK_4.equals(block)) {
            throw new UnsupportedOperationException("Delete action only supported for block 4");
        }

        SwiftBlock4 block4 = message.getBlock4();
        if (block4 == null) return;

        String targetSequence = transformation.sequence();

        for (FieldConfig fieldConfig : transformation.fields()) {
            String fieldSpec = fieldConfig.field();

            // Parse field specification
            String tag;
            String qualifier = null;

            if (fieldSpec.contains(QUALIFIER_SEPARATOR)) {
                String[] parts = fieldSpec.split(QUALIFIER_SEPARATOR);
                tag = parts[0];
                qualifier = parts[1];
            } else {
                tag = fieldSpec;
            }

            // Get the appropriate block to search in
            SwiftTagListBlock searchBlock;
            if (targetSequence != null) {
                // Get the subblock for the specific sequence
                searchBlock = block4.getSubBlock(targetSequence);
                if (searchBlock == null) {
                    log.error("No block found for target sequence {}, skipping deletion of {}", targetSequence, fieldSpec);
                    continue; // Sequence doesn't exist, skip to next field
                }
            } else {
                // No sequence specified, search entire block4
                searchBlock = block4;
            }

            // Find and remove matching tags
            List<Tag> tagsToRemove = new ArrayList<>();
            for (Tag existingTag : searchBlock.getTags()) {
                if (existingTag.getName().equals(tag)) {
                    if (qualifier != null) {
                        Field field = Field.getField(existingTag);
                        String fieldQualifier = field.getComponent(1);
                        if (qualifier.equals(fieldQualifier)) {
                            tagsToRemove.add(existingTag);
                        }
                    } else {
                        tagsToRemove.add(existingTag);
                    }
                }
            }

            // Remove from the actual block4 tags list (not from searchBlock which is just a view)
            block4.getTags().removeAll(tagsToRemove);
        }
    }

    /**
     * Execute DELETE SEQUENCE action - remove entire sequences.
     */
    public void executeDeleteSequence(Transformation transformation, SwiftMessage message) {
        String block = transformation.block();

        if (!BLOCK_4.equals(block)) {
            throw new UnsupportedOperationException("DeleteSequence action only supported for block 4");
        }

        SwiftBlock4 block4 = message.getBlock4();
        if (block4 == null) return;

        String parentSequence = transformation.sequence();

        for (SequenceConfig seqConfig : transformation.sequences()) {
            String sequenceName = seqConfig.sequence();

            if (parentSequence != null) {
                // Delete sequence within a parent sequence
                // We need to manually find and remove since getSubBlock returns a copy
                List<Tag> tagsToRemove = new ArrayList<>();
                boolean inParent = false;
                boolean inTarget = false;

                for (Tag tag : block4.getTags()) {
                    // Track parent sequence
                    if (tag.getName().equals(SEQUENCE_START_MARKER) && parentSequence.equals(tag.getValue())) {
                        inParent = true;
                    }

                    // Track target sequence (only if we're in the parent)
                    if (inParent && tag.getName().equals(SEQUENCE_START_MARKER) && sequenceName.equals(tag.getValue())) {
                        inTarget = true;
                    }

                    // Collect tags to remove if we're in the target sequence
                    if (inTarget) {
                        tagsToRemove.add(tag);
                    }

                    // Track when we exit the target sequence
                    if (inTarget && tag.getName().equals(SEQUENCE_END_MARKER) && sequenceName.equals(tag.getValue())) {
                        inTarget = false;
                    }

                    // Track when we exit the parent sequence
                    if (inParent && tag.getName().equals(SEQUENCE_END_MARKER) && parentSequence.equals(tag.getValue())) {
                        inParent = false;
                    }
                }

                if (tagsToRemove.isEmpty()) {
                    log.error("Sequence {} not found within parent sequence {}", sequenceName, parentSequence);
                }

                block4.getTags().removeAll(tagsToRemove);
            } else {
                // No parent sequence specified, remove from entire block4
                block4.removeSubBlock(sequenceName);
            }
        }
    }
}

