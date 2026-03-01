package com.abnamro.mpm.swift.mx.dsl;

/**
 * A single transformation to apply to the target XML template.
 *
 * @param xpath  XPath expression locating the node to modify in the target template
 * @param value  New value to set; may contain ${variableName} references
 */
public record MxTransformation(String xpath, String value) {
}
