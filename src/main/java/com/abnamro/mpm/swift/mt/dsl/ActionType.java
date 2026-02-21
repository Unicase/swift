package com.abnamro.mpm.swift.mt.dsl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionType {
    SET("set"),
    DELETE("delete"),
    DELETE_SEQUENCE("deleteSequence"),
    INSERT_BEFORE("insertBefore"),
    INSERT_AFTER("insertAfter");

    private final String value;

    ActionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ActionType fromValue(String value) {
        for (ActionType type : ActionType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown action type: " + value);
    }
}
