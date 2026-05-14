package com.tecdes.smart.app_smart_40.dto;

import com.tecdes.smart.app_smart_40.model.Expedicao;

import lombok.Builder;

@Builder
public record ExpedicaoResponseDTO(
        Long idExpedicao,
        Long nrPosicao,
        Long idPedido,
        Long nrOrdemProducao
) {
    public static ExpedicaoResponseDTO fromEntity(Expedicao expedicao) {
        return new ExpedicaoResponseDTO(
                expedicao.getIdExpedicao(),
                expedicao.getNrPosicao(),
                expedicao.getPedido().getIdPedido(),
                expedicao.getPedido().getNrOrdemProducao()
        );
    }
}