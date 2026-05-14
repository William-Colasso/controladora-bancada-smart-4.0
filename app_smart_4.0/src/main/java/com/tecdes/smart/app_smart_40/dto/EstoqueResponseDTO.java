package com.tecdes.smart.app_smart_40.dto;

import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

public record EstoqueResponseDTO(
        Long id,
        Integer posicao,
        CorBloco corBloco
) {
    public static EstoqueResponseDTO fromEntity(Estoque estoque) {
        return new EstoqueResponseDTO(
                estoque.getId(),
                estoque.getPosicao(),
                estoque.getCorBloco()
        );
    }

    
}