package com.tecdes.smart.app_smart_40.dto;

import com.tecdes.smart.app_smart_40.model.enums.CorLamina;
import com.tecdes.smart.app_smart_40.model.enums.PadraoLamina;
import com.tecdes.smart.app_smart_40.model.enums.PosicaoLamina;

public record LaminaDTO(
        CorLamina cor,
        PadraoLamina padrao,
        PosicaoLamina posicaoNoBloco) {

    public static LaminaDTO fromEntity(com.tecdes.smart.app_smart_40.model.Lamina lamina) {
        return new LaminaDTO(
                lamina.getCor(),
                lamina.getPadrao(),
                lamina.getPosicaoNoBloco());
    }

    public com.tecdes.smart.app_smart_40.model.Lamina toEntity() {
        return com.tecdes.smart.app_smart_40.model.Lamina.builder()
                .cor(this.cor)
                .padrao(this.padrao)
                .posicaoNoBloco(this.posicaoNoBloco)
                .build();
    }
}