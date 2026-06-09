package com.tecdes.smart.app_smart_40.dto.request;

import com.tecdes.smart.app_smart_40.model.Lamina;
import com.tecdes.smart.app_smart_40.model.enums.CorLamina;
import com.tecdes.smart.app_smart_40.model.enums.PadraoLamina;
import com.tecdes.smart.app_smart_40.model.enums.PosicaoLamina;

public record LaminaRequestDTO(
        CorLamina cor,
        PadraoLamina padrao,
        PosicaoLamina posicao) {

    public Lamina toEntity() {
        return Lamina.builder()
                .cor(this.cor)
                .padrao(this.padrao)
                .posicao(this.posicao)
                .build();
    }
}
