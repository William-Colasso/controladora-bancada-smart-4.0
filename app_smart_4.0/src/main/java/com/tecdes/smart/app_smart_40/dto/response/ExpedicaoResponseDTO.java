package com.tecdes.smart.app_smart_40.dto.response;

import com.tecdes.smart.app_smart_40.model.Expedicao;

import lombok.Builder;
import lombok.Data;

@Builder

public record ExpedicaoResponseDTO(
        Long id,
        Integer posicao,
        PedidoResponseDTO pedidoResponseDTO) {

    public static ExpedicaoResponseDTO fromEntity(Expedicao expedicao) {
        return new ExpedicaoResponseDTO(
                expedicao.getId(),
                expedicao.getPosicao(),
                expedicao.getPedidoAtual() != null ? PedidoResponseDTO.fromEntity(expedicao.getPedidoAtual()) : null);
    }

    public Expedicao toEntity() {
        Expedicao expedicao = new Expedicao();
        expedicao.setId(this.id);
        expedicao.setPosicao(this.posicao);
        if (this.pedidoResponseDTO != null) {
            expedicao.setPedidoAtual(this.pedidoResponseDTO.toEntity());
        }
        return expedicao;
    }
}
