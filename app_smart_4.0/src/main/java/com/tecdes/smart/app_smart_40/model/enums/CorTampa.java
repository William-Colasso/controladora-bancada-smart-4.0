package com.tecdes.smart.app_smart_40.model.enums;

public enum CorTampa {
    PRETO(1),
    VERMELHO(2),
    AZUL(3);

    private final Integer value;

    CorTampa(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
