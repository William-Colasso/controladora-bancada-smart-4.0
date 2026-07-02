package com.tecdes.smart.app_smart_40.dto.request;

import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

public record EstoqueRequestDTO(
        Integer posicao,
        CorBloco corBloco) {

    public Estoque toEntity() {
        return new Estoque(null, this.posicao, this.corBloco, null);
    }
}
