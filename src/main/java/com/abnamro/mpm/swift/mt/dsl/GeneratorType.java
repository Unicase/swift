package com.abnamro.mpm.swift.mt.dsl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GeneratorType {
    NUMERIC("numeric"),
    ALPHANUMERIC("alphanumeric"),
    DATE("date"),
    TIME("time");

    private final String value;

    GeneratorType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static GeneratorType fromValue(String value) {
        for (GeneratorType type : GeneratorType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown generator type: " + value);
    }
}
