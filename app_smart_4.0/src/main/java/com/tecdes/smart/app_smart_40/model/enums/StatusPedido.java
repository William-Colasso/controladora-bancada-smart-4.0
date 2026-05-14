package com.tecdes.smart.app_smart_40.model.enums;

import jakarta.persistence.EnumeratedValue;

public enum StatusPedido {

    PENDENTE(1),
    PRODUCAO(2),
    CONCLUIDO(3);

    @EnumeratedValue
    private final int value;

    StatusPedido(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}