package com.tecdes.smart.app_smart_40.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum CorBloco {
    VAZIO(0),
    PRETO(1),
    VERMELHO(2),
    AZUL(3);

    @EnumeratedValue
    private final int value;

    CorBloco(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static CorBloco fromValue(int value) {
        for (CorBloco c : CorBloco.values()) {
            if (c.getValue() == value) {
                return c;
            }
        }
        throw new IllegalArgumentException("Cor de bloco inválida: " + value);
    }
}