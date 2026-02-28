package com.abnamro.mpm.swift.mx.dsl;

/**
 * Variable extractor for MX messages.
 * Extracts a value from the source XML document using an XPath expression.
 *
 * @param id       Variable identifier, used in ${id} references
 * @param xpath    XPath expression to locate the value in the source XML
 * @param required Whether a missing value causes an error (defaults to true)
 */
public record MxVariable(String id, String xpath, boolean required) {

    public MxVariable(String id, String xpath) {
        this(id, xpath, true);
    }
}
