package com.tecdes.smart.app_smart_40.dto.response;

import com.tecdes.smart.app_smart_40.model.Lamina;
import com.tecdes.smart.app_smart_40.model.enums.CorLamina;
import com.tecdes.smart.app_smart_40.model.enums.PadraoLamina;
import com.tecdes.smart.app_smart_40.model.enums.PosicaoLamina;

public record LaminaResponseDTO(
        CorLamina cor,
        PadraoLamina padrao,
        PosicaoLamina posicaoNoBloco) {

    public static LaminaResponseDTO fromEntity(Lamina lamina) {
        return new LaminaResponseDTO(
                lamina.getCor(),
                lamina.getPadrao(),
                lamina.getPosicaoNoBloco());
    }

    public Lamina toEntity() {
        return Lamina.builder()
                .cor(this.cor)
                .padrao(this.padrao)
                .posicaoNoBloco(this.posicaoNoBloco)
                .build();
    }
}
