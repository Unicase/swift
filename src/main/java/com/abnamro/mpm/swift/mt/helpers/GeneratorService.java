package com.abnamro.mpm.swift.mt.helpers;

import com.abnamro.mpm.swift.mt.dsl.Generator;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating values based on generator configurations.
 * Supports: numeric, alphanumeric, time, date
 */
public class GeneratorService {
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String NUMERIC_CHARS = "0123456789";
    private final SecureRandom random = new SecureRandom();

    /**
     * Generate all values for the configured generators.
     *
     * @param generators List of generator configurations
     * @return Map of generator ID to generated value
     */
    public Map<String, String> generateAll(List<Generator> generators) {
        Map<String, String> values = new HashMap<>();
        generators.forEach(generator -> {
            String value = generate(generator);
            values.put(generator.id(), value);
        });
        return values;
    }

    /**
     * Generate a value based on the generator configuration.
     */
    private String generate(Generator generator) {
        return switch (generator.type()) {
            case NUMERIC -> generateNumeric(generator.length());
            case ALPHANUMERIC -> generateAlphanumeric(generator.length());
            case TIME -> generateTime(generator.format());
            case DATE -> generateDate(generator.format());
        };
    }

    /**
     * Generate a numeric string of the specified length.
     */
    private String generateNumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(NUMERIC_CHARS.charAt(random.nextInt(NUMERIC_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Generate an alphanumeric string of the specified length.
     */
    private String generateAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC_CHARS.charAt(random.nextInt(ALPHANUMERIC_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Generate current time in the specified format.
     */
    private String generateTime(String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalTime.now().format(formatter);
    }

    /**
     * Generate current date in the specified format.
     */
    private String generateDate(String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDate.now().format(formatter);
    }
}