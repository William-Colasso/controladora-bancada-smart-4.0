package com.tecdes.smart.app_smart_40.model.enums;

import jakarta.persistence.EnumeratedValue;

public enum PosicaoLamina {
    ESQUERDA(1),
    FRENTE(2),
    DIREITA(3);


    @EnumeratedValue
    private final int value;

    PosicaoLamina(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}