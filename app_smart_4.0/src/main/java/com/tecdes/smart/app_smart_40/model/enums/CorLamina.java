package com.tecdes.smart.app_smart_40.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum CorLamina {
    VERMELHO(1),
    AZUL(2),
    AMARELO(3),
    VERDE(4),
    PRETO(5),
    BRANCO(6);


    @EnumeratedValue
    private final int value;

    CorLamina(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static CorLamina fromValue(int value) {
        for (CorLamina c : CorLamina.values()) {
            if (c.getValue() == value) {
                return c;
            }
        }
        throw new IllegalArgumentException("Cor de lâmina inválida: " + value);
    }
}
