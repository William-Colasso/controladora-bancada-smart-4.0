package com.tecdes.smart.app_smart_40.dto;

import com.tecdes.smart.app_smart_40.model.Expedicao;

import lombok.Builder;

@Builder
public record ExpedicaoResponseDTO(
        Long idExpedicao,
        PedidoDTO pedidoDTO,
        Integer posicao) {
    public static ExpedicaoResponseDTO fromEntity(Expedicao expedicao) {
        return new ExpedicaoResponseDTO(
                expedicao.getId(),
                PedidoDTO.fromEntity(expedicao.getPedido()),
                expedicao.getPosicao());
    }
}