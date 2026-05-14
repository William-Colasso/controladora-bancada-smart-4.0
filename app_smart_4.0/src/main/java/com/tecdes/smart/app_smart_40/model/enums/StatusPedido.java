package com.tecdes.smart.app_smart_40.model.enums;

public enum StatusPedido {

    PENDENTE(1),
    PRODUCAO(2),
    CONCLUIDO(3);

    private final Integer value;

    StatusPedido(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}