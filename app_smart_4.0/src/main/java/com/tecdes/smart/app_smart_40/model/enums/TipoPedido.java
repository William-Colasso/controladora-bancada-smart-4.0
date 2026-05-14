package com.tecdes.smart.app_smart_40.model.enums;

public enum TipoPedido {
    SIMPLES(1),
    DUPLO(2),
    TRIPLO(3);

    private final Integer value;

    TipoPedido(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
