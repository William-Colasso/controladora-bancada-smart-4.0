package com.tecdes.smart.app_smart_40.dto;

import com.tecdes.smart.app_smart_40.model.Estoque;

public record EstoqueResponseDTO(
        Long idEstoque,
        Byte nrPosicao,
        Byte vlCorBloco
) {
    public static EstoqueResponseDTO fromEntity(Estoque estoque) {
        return new EstoqueResponseDTO(
                estoque.getIdEstoque(),
                estoque.getNrPosicao(),
                estoque.getVlCorBloco()
        );
    }

    
}