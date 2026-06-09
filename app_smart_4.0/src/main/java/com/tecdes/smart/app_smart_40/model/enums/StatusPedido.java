package com.tecdes.smart.app_smart_40.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static StatusPedido fromValue(int value) {
        for (StatusPedido s : StatusPedido.values()) {
            if (s.getValue() == value) {
                return s;
            }
        }
        throw new IllegalArgumentException("Status de pedido inválido: " + value);
    }
}