package com.tecdes.smart.app_smart_40.model.enums;

public enum CorBloco {
    PRETO(1),
    VERMELHO(2),
    AZUL(3);

    private final Integer value;

    CorBloco(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}