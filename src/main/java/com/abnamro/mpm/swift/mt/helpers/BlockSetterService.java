package com.abnamro.mpm.swift.mt.helpers;

import com.abnamro.mpm.swift.mt.dsl.*;
import com.prowidesoftware.swift.model.*;
import com.prowidesoftware.swift.model.field.Field;

import java.util.Map;

import static com.abnamro.mpm.swift.mt.SwiftConstants.*;

/**
 * Service for setting field values in SWIFT message blocks.
 */
public class BlockSetterService {

    private final FieldResolver fieldResolver;

    public BlockSetterService(FieldResolver fieldResolver) {
        this.fieldResolver = fieldResolver;
    }

    /**
     * Execute SET action for the appropriate block.
     */
    public void executeSet(Transformation transformation, SwiftMessage message, Map<String, String> context) {
        switch (transformation.block()) {
            case BLOCK_1:
                setBlock1(transformation, message.getBlock1(), context);
                break;
            case BLOCK_2:
                setBlock2(transformation, message.getBlock2(), context);
                break;
            case BLOCK_3:
                setBlock3(transformation, message.getBlock3(), context);
                break;
            case BLOCK_4:
                setBlock4(transformation, message.getBlock4(), context);
                break;
            case BLOCK_5:
                setBlock5(transformation, message, context);
                break;
            case BLOCK_S:
                setSystemBlock(transformation, message, context);
                break;
            default:
                throw new IllegalArgumentException("Unknown block: " + transformation.block());
        }
    }

    /**
     * Set fields in Block 1 (Basic Header).
     */
    private void setBlock1(Transformation transformation, SwiftBlock1 block1, Map<String, String> context) {
        if (block1 == null) return;

        for (FieldConfig field : transformation.fields()) {
            String value = fieldResolver.resolveValue(field, context);
            String fieldName = field.field();

            switch (fieldName.toLowerCase()) {
                case "sessionnumber":
                    block1.setSessionNumber(value);
                    break;
                case "sequencenumber":
                    block1.setSequenceNumber(value);
                    break;
                case "logicalterminal":
                    block1.setLogicalTerminal(value);
                    break;
            }
        }
    }

    /**
     * Set fields in Block 2 (Application Header).
     */
    private void setBlock2(Transformation transformation, SwiftBlock2 block2, Map<String, String> context) {
        if (block2 == null) return;

        for (FieldConfig field : transformation.fields()) {
            String value = fieldResolver.resolveValue(field, context);
            String fieldName = field.field();

            if (block2 instanceof SwiftBlock2Output output) {
                switch (fieldName.toLowerCase()) {
                    case "messagetype":
                        output.setMessageType(value);
                        break;
                    case "senderinputtime":
                        output.setSenderInputTime(value);
                        break;
                    case "senderinputdate":
                        output.setMIRDate(value);
                        break;
                    case "receiveroutputdate":
                        output.setReceiverOutputDate(value);
                        break;
                    case "receiveroutputtime":
                        output.setReceiverOutputTime(value);
                        break;
                    case "sessionnumber":
                        output.setMIRSessionNumber(value);
                        break;
                    case "sequencenumber":
                        output.setMIRSequenceNumber(value);
                        break;
                }
            } else if (block2 instanceof SwiftBlock2Input input) {
                switch (fieldName.toLowerCase()) {
                    case "messagetype":
                        input.setMessageType(value);
                        break;
                    case "receiveraddress":
                        input.setReceiverAddress(value);
                        break;
                }
            }
        }
    }

    /**
     * Set fields in Block 3 (User Header).
     */
    private void setBlock3(Transformation transformation, SwiftBlock3 block3, Map<String, String> context) {
        if (block3 == null) return;

        for (FieldConfig field : transformation.fields()) {
            String value = fieldResolver.resolveValue(field, context);
            String tagName = field.field();

            // Remove existing tag if present
            Tag existingTag = block3.getTagByName(tagName);
            if (existingTag != null) {
                block3.getTags().remove(existingTag);
            }

            // Add new tag
            block3.append(new Tag(tagName, value));
        }
    }

    /**
     * Set fields in Block 4 (Text Block / Message Body).
     */
    private void setBlock4(Transformation transformation, SwiftBlock4 block4, Map<String, String> context) {
        if (block4 == null) return;

        String targetSequence = transformation.sequence();

        for (FieldConfig fieldConfig : transformation.fields()) {
            String fieldSpec = fieldConfig.field();
            String value = fieldResolver.resolveValue(fieldConfig, context);

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

            // Find and update the field (optionally filtered by sequence)
            String currentSequence = null;

            for (Tag existingTag : block4.getTags()) {
                // Track current sequence
                if (existingTag.getName().equals(SEQUENCE_START_MARKER)) {
                    currentSequence = existingTag.getValue();
                } else if (existingTag.getName().equals(SEQUENCE_END_MARKER)) {
                    currentSequence = null;
                }

                if (existingTag.getName().equals(tag)) {
                    // If a target sequence is specified, only update within that sequence
                    if (targetSequence != null && !targetSequence.equals(currentSequence)) {
                        continue; // Skip this tag, it's not in the target sequence
                    }

                    Field field = Field.getField(existingTag);

                    // Check qualifier match if specified
                    if (qualifier != null) {
                        String fieldQualifier = field.getComponent(1);
                        if (!qualifier.equals(fieldQualifier)) {
                            continue;
                        }
                        // Update qualified field - set component 2 (value)
                        field.setComponent(2, value);
                    } else {
                        // Simple field - set the value
                        field.setComponent(1, value);
                    }

                    existingTag.setValue(field.getValue());
                    break;
                }
            }
        }
    }

    /**
     * Set fields in Block 5 (Trailer).
     */
    private void setBlock5(Transformation transformation, SwiftMessage message, Map<String, String> context) {
        // Create Block 5 if it doesn't exist
        SwiftBlock5 block5 = message.getBlock5();
        if (block5 == null) {
            block5 = new SwiftBlock5();
            message.setBlock5(block5);
        }

        for (FieldConfig field : transformation.fields()) {
            String value = fieldResolver.resolveValue(field, context);
            String tagName = field.field();

            Tag existingTag = block5.getTagByName(tagName);
            if (existingTag != null) {
                existingTag.setValue(value);
            } else {
                block5.append(new Tag(tagName, value));
            }
        }
    }

    /**
     * Set fields in System Block (S block).
     */
    private void setSystemBlock(Transformation transformation, SwiftMessage message, Map<String, String> context) {
        // System block handling - create user block if needed
        SwiftBlockUser systemBlock = message.getUserBlock("S");
        if (systemBlock == null) {
            systemBlock = new SwiftBlockUser("S");
            message.addUserBlock(systemBlock);
        }

        for (FieldConfig field : transformation.fields()) {
            String value = fieldResolver.resolveValue(field, context);
            String tagName = field.field();

            Tag existingTag = systemBlock.getTagByName(tagName);
            if (existingTag != null) {
                existingTag.setValue(value);
            } else {
                systemBlock.append(new Tag(tagName, value));
            }
        }
    }
}

