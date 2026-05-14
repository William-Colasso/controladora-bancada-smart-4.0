package com.tecdes.smart.app_smart_40.model.enums;

import jakarta.persistence.EnumeratedValue;

public enum PadraoLamina {
    NENHUM(0),
    CASA(1),
    NAVIO(2),
    ESTRELA(3);

    @EnumeratedValue
    private final int value;

    PadraoLamina(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}