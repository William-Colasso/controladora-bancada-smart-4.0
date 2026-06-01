package com.tecdes.smart.app_smart_40.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.EnumeratedValue;

public enum TipoPedido {
    SIMPLES(1),
    DUPLO(2),
    TRIPLO(3);

    @EnumeratedValue
    private final int value;

    TipoPedido(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static TipoPedido fromValue(int value) {
        for (TipoPedido t : TipoPedido.values()) {
            if (t.getValue() == value) {
                return t;
            }
        }
        throw new IllegalArgumentException("Tipo de pedido inválido: " + value);
    }
}
