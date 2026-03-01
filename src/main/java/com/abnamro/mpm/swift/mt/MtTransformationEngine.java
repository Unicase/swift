package com.abnamro.mpm.swift.mt;

import com.abnamro.mpm.swift.common.helpers.FieldResolver;
import com.abnamro.mpm.swift.common.helpers.GeneratorService;
import com.abnamro.mpm.swift.mt.dsl.*;
import com.abnamro.mpm.swift.mt.helpers.*;
import com.prowidesoftware.swift.model.*;

import java.util.*;

/**
 * Engine that executes MT message transformations based on the DSL specification.
 * <p>
 * Takes a source SwiftMessage and a TransformationSpec, produces a target SwiftMessage.
 * <p>
 * This class orchestrates the transformation by delegating to specialized service classes:
 * - BlockSetterService: Handles SET operations
 * - BlockDeleteService: Handles DELETE and DELETE_SEQUENCE operations
 * - BlockInsertService: Handles INSERT_AFTER and INSERT_BEFORE operations
 * - MessageCopier: Handles message copying
 * - FieldResolver: Handles value resolution
 */
public class MtTransformationEngine {
    private final GeneratorService generatorService;
    private final VariableExtractor variableExtractor;
    private final MessageCopier messageCopier;
    private final BlockSetterService blockSetterService;
    private final BlockDeleteService blockDeleteService;
    private final BlockInsertService blockInsertService;

    public MtTransformationEngine() {
        this.generatorService = new GeneratorService();
        this.variableExtractor = new VariableExtractor();
        this.messageCopier = new MessageCopier();
        FieldResolver fieldResolver = new FieldResolver();
        this.blockSetterService = new BlockSetterService(fieldResolver);
        this.blockDeleteService = new BlockDeleteService();
        this.blockInsertService = new BlockInsertService(fieldResolver);
    }

    /**
     * Execute a transformation specification against a source message.
     *
     * @param spec          The transformation specification
     * @param sourceMessage The source SWIFT message
     * @return The transformed target message
     */
    public SwiftMessage transform(MtTransformationSpec spec, SwiftMessage sourceMessage) {
        // Start with a copy of the source message
        SwiftMessage targetMessage = messageCopier.copyMessage(sourceMessage);

        // Generate all dynamic values
        Map<String, String> generatedValues = generatorService.generateAll(spec.getGenerators());

        // Extract all variable values from source
        Map<String, String> variableValues = variableExtractor.extractAll(
                spec.getVariables(),
                sourceMessage.getBlock4()
        );

        // Combine into single context for variable resolution
        Map<String, String> context = new HashMap<>();
        context.putAll(generatedValues);
        context.putAll(variableValues);

        // Execute each transformation in order
        for (MtTransformation mtTransformation : spec.getTransformations()) {
            executeTransformation(mtTransformation, targetMessage, context);
        }

        return targetMessage;
    }

    /**
     * Execute a single transformation action by delegating to appropriate service.
     */
    private void executeTransformation(MtTransformation mtTransformation, SwiftMessage message, Map<String, String> context) {
        switch (mtTransformation.action()) {
            case SET:
                blockSetterService.executeSet(mtTransformation, message, context);
                break;
            case DELETE:
                blockDeleteService.executeDelete(mtTransformation, message);
                break;
            case DELETE_SEQUENCE:
                blockDeleteService.executeDeleteSequence(mtTransformation, message);
                break;
            case INSERT_AFTER:
                blockInsertService.executeInsertAfter(mtTransformation, message, context);
                break;
            case INSERT_BEFORE:
                blockInsertService.executeInsertBefore(mtTransformation, message, context);
                break;
            default:
                throw new IllegalArgumentException("Unknown action: " + mtTransformation.action());
        }
    }
}

