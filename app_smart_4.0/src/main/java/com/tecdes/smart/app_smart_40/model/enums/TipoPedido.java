package com.tecdes.smart.app_smart_40.model.enums;

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

    public int getValue() {
        return value;
    }
}
