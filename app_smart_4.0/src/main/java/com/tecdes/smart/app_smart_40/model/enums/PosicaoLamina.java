package com.tecdes.smart.app_smart_40.model.enums;

public enum PosicaoLamina {
    ESQUERDA(1),
    FRENTE(2),
    DIREITA(3);

    private final Integer value;

    PosicaoLamina(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}