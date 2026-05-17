package com.tecdes.smart.app_smart_40.dto;

import java.util.List;

import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

public record EstoqueDTO(
        Long id,
        Integer posicao,
        CorBloco corBloco) {
    public static EstoqueDTO fromEntity(Estoque estoque) {
        return new EstoqueDTO(
                estoque.getId(),
                estoque.getPosicao(),
                estoque.getCorBloco());
    }

    public static Estoque toEntity(EstoqueDTO estoqueDTO) {
        return new Estoque(estoqueDTO.id(), estoqueDTO.posicao(), estoqueDTO.corBloco(), null);
    }
}