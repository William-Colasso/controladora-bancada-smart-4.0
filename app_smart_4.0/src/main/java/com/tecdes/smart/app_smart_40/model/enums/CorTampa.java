package com.tecdes.smart.app_smart_40.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum CorTampa {
    PRETO(1),
    VERMELHO(2),
    AZUL(3);

    @EnumeratedValue
    private final int value;

    CorTampa(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static CorTampa fromValue(int value) {
        for (CorTampa c : CorTampa.values()) {
            if (c.getValue() == value) {
                return c;
            }
        }
        throw new IllegalArgumentException("Cor de tampa inválida: " + value);
    }
}
