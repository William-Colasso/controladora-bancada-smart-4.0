package com.tecdes.smart.app_smart_40.dto.request;

import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;

public record ExpedicaoRequestDTO(
    Integer posicao
    ) {

    public  Expedicao toEntity(ExpedicaoRequestDTO exp) {
        return Expedicao.builder()
        .posicao(exp.posicao())
        .build();
    }
}
