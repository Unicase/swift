package com.abnamro.mpm.swift.mt.helpers;

import com.abnamro.mpm.swift.mt.dsl.MtVariable;
import com.abnamro.mpm.swift.mt.dsl.VariableSource;
import com.prowidesoftware.swift.model.SwiftBlock4;
import com.prowidesoftware.swift.model.field.Field;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.abnamro.mpm.swift.mt.SwiftConstants.BLOCK_4;
import static com.abnamro.mpm.swift.mt.SwiftConstants.QUALIFIER_SEPARATOR;

/**
 * Service for extracting variable values from a source SWIFT message.
 */
public class VariableExtractor {

    /**
     * Extract all variable values from the source message.
     *
     * @param mtVariables List of variable definitions
     * @param block4    The source message block 4
     * @return Map of variable ID to extracted value
     */
    public Map<String, String> extractAll(List<MtVariable> mtVariables, SwiftBlock4 block4) {
        Map<String, String> values = new HashMap<>();

        for (MtVariable mtVariable : mtVariables) {
            String value = extract(mtVariable, block4);

            if (value == null && mtVariable.required()) {
                throw new IllegalStateException(
                    "Required variable '" + mtVariable.id() + "' not found in source message"
                );
            }

            if (value != null) {
                values.put(mtVariable.id(), value);
            }
        }

        return values;
    }

    /**
     * Extract a single variable value from the source message.
     */
    private String extract(MtVariable mtVariable, SwiftBlock4 block4) {
        VariableSource source = mtVariable.source();

        // Only block 4 extraction is supported for now
        if (!BLOCK_4.equals(source.block())) {
            throw new UnsupportedOperationException(
                "Variable extraction from block " + source.block() + " is not supported"
            );
        }

        String fieldSpec = source.field();

        // Parse field specification (e.g., "20C::SEME" -> tag "20C", qualifier "SEME")
        String tag;
        String qualifier = null;

        if (fieldSpec.contains(QUALIFIER_SEPARATOR)) {
            String[] parts = fieldSpec.split(QUALIFIER_SEPARATOR);
            tag = parts[0];
            qualifier = parts[1];
        } else {
            tag = fieldSpec;
        }

        // Find matching field(s)
        Field[] fields = block4.getFieldsByName(tag);
        if (fields == null) {
            return null;
        }

        // If qualifier specified, filter by it
        for (Field field : fields) {
            if (qualifier != null) {
                // Check if field has matching qualifier in component 1
                String fieldQualifier = field.getComponent(1);
                if (qualifier.equals(fieldQualifier)) {
                    // Return the complete value after the qualifier (all remaining components)
                    // For qualified fields like :SEME//REF123, we want everything after the qualifier
                    String fullValue = field.getValue();
                    if (fullValue != null && fullValue.contains("//")) {
                        // Return everything after the "//" separator
                        return fullValue.substring(fullValue.indexOf("//") + 2);
                    }
                    // Fallback to component 2 if no "//" separator found
                    return field.getComponent(2);
                }
            } else {
                // No qualifier filter, return first match value
                return field.getValue();
            }
        }

        return null;
    }
}

