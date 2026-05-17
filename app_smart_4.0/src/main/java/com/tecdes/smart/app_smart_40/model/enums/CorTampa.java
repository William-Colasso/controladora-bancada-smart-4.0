package com.tecdes.smart.app_smart_40.model.enums;

import jakarta.persistence.EnumeratedValue;

public enum CorTampa {
    PRETO(1),
    VERMELHO(2),
    AZUL(3);

    @EnumeratedValue
    private final int value;

    CorTampa(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
