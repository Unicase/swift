package com.abnamro.mpm.swift.mt.dsl;

/**
 * Defines where to extract a variable value from in the source message.
 *
 * @param block    Block name (1-5)
 * @param sequence Optional sequence name (for block 4)
 * @param field    Field identifier
 */
public record VariableSource(String block, String sequence, String field) {
}

