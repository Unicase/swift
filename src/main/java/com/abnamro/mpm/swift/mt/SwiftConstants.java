package com.abnamro.mpm.swift.mt;

/**
 * Constants for SWIFT message processing.
 */
public final class SwiftConstants {

    private SwiftConstants() {
        // Private constructor to prevent instantiation
    }

    /**
     * SWIFT sequence boundary markers
     */
    public static final String SEQUENCE_START_MARKER = "16R";
    public static final String SEQUENCE_END_MARKER = "16S";

    /**
     * Common SWIFT block identifiers
     */
    public static final String BLOCK_1 = "1";
    public static final String BLOCK_2 = "2";
    public static final String BLOCK_3 = "3";
    public static final String BLOCK_4 = "4";
    public static final String BLOCK_5 = "5";
    public static final String BLOCK_S = "S";

    /**
     * Field qualifiers separator
     */
    public static final String QUALIFIER_SEPARATOR = "::";
}

