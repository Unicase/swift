package com.abnamro.mpm.swift.mt.dsl;

/**
 * Configuration for a single field within a transformation.
 *
 * @param field Field identifier (e.g., "20C::SEME", "messageType", "108")
 * @param value Value or variable reference (e.g., "${senderMessageReference}")
 */
public record FieldConfig(String field, String value) {

    /**
     * Check if the value is a variable reference (${...})
     */
    public boolean isVariableReference() {
        return value != null && value.startsWith("${") && value.endsWith("}");
    }

    /**
     * Extract the variable name from a variable reference.
     *
     * @return The variable name without ${} wrapper, or null if not a variable reference
     */
    public String getVariableName() {
        if (isVariableReference()) {
            return value.substring(2, value.length() - 1);
        }
        return null;
    }
}