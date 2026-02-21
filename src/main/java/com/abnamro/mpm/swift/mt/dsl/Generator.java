package com.abnamro.mpm.swift.mt.dsl;

/**
 * Generator configuration for creating dynamic values.
 * Supports types: numeric, alphanumeric, time, date
 *
 * @param id     Generator identifier
 * @param type   Generator type (NUMERIC, ALPHANUMERIC, TIME, DATE)
 * @param length Length for numeric/alphanumeric generators
 * @param format Format string for time/date generators
 */
public record Generator(String id, GeneratorType type, Integer length, String format) {
}

