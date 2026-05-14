package com.tecdes.smart.app_smart_40.model.enums;

public enum PadraoLamina {
    NENHUM(0),
    CASA(1),
    NAVIO(2),
    ESTRELA(3);

    private final Integer value;

    PadraoLamina(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}