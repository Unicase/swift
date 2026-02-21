package com.abnamro.mpm.swift.mt.dsl;

/**
 * Variable definition for extracting values from the source message.
 *
 * @param id       Variable identifier
 * @param source   Source location in the message
 * @param required Whether this variable is required
 */
public record Variable(String id, VariableSource source, boolean required) {
}

