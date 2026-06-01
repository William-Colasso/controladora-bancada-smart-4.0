package com.tecdes.smart.app_smart_40.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum AndarBloco {
    PRIMEIRO(1),
    SEGUNDO(2),
    TERCEIRO(3);

    @EnumeratedValue
    private final int value;

    AndarBloco(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return this.value;
    }

    @JsonCreator
    public static AndarBloco fromValue(int value) {
        for (AndarBloco a : AndarBloco.values()) {
            if (a.getValue() == value) {
                return a;
            }
        }
        throw new IllegalArgumentException("Andar inválido: " + value);
    }

}
