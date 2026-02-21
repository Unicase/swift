package com.abnamro.mpm.swift.mt.helpers;

import com.abnamro.mpm.swift.mt.dsl.FieldConfig;

import java.util.Map;

/**
 * Service for resolving field values and variable references.
 */
public class FieldResolver {

    /**
     * Resolve a FieldConfig's value, replacing variable references with actual values from context.
     * This method uses the FieldConfig helper methods for better encapsulation.
     *
     * @param fieldConfig The field configuration containing the value to resolve
     * @param context     Map of variable names to their values
     * @return The resolved value with all variables replaced
     */
    public String resolveValue(FieldConfig fieldConfig, Map<String, String> context) {
        if (fieldConfig == null || fieldConfig.value() == null) {
            return null;
        }

        // Use FieldConfig helper methods to check if this is a variable reference
        if (fieldConfig.isVariableReference()) {
            String variableName = fieldConfig.getVariableName();
            // Look up the variable in the context and return its value, or the original if not found
            return context.getOrDefault(variableName, fieldConfig.value());
        }

        // Not a simple variable reference, might contain multiple ${...} patterns
        return resolveValue(fieldConfig.value(), context);
    }

    /**
     * Resolve a value string, replacing all variable references with actual values from context.
     * Supports multiple ${...} patterns in a single value string.
     *
     * @param value   The value string that may contain ${variableName} patterns
     * @param context Map of variable names to their values
     * @return The resolved value with all variables replaced
     */
    public String resolveValue(String value, Map<String, String> context) {
        if (value == null) {
            return null;
        }

        // Replace all ${...} references
        String result = value;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }

        return result;
    }
}

