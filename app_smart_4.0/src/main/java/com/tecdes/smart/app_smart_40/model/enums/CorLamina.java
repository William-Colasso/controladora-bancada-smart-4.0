package com.tecdes.smart.app_smart_40.model.enums;

import jakarta.persistence.EnumeratedValue;

public enum CorLamina {
    VERMELHO(1),
    AZUL(2),
    AMARELO(3),
    VERDE(4),
    PRETO(5),
    BRANCO(6);


    @EnumeratedValue
    private final int value;

    CorLamina(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
