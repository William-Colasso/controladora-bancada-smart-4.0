package com.tecdes.smart.app_smart_40.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum PadraoLamina {
    NENHUM(0),
    CASA(1),
    NAVIO(2),
    ESTRELA(3);

    @EnumeratedValue
    private final int value;

    PadraoLamina(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static PadraoLamina fromValue(int value) {
        for (PadraoLamina p : PadraoLamina.values()) {
            if (p.getValue() == value) {
                return p;
            }
        }
        throw new IllegalArgumentException("Padrão de lâmina inválido: " + value);
    }
}