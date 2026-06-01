package com.tecdes.smart.app_smart_40.dto.response;

import com.tecdes.smart.app_smart_40.model.Expedicao;

import lombok.Builder;

@Builder
public record ExpedicaoResponseDTO(
        Long id,
        Integer posicao,
        PedidoResponseDTO pedidoResponseDTO) {

    public static ExpedicaoResponseDTO fromEntity(Expedicao expedicao) {
        return new ExpedicaoResponseDTO(
                expedicao.getId(),
                expedicao.getPosicao(),
                expedicao.getPedido() != null ? PedidoResponseDTO.fromEntity(expedicao.getPedido()) : null);
    }

    public Expedicao toEntity() {
        Expedicao expedicao = new Expedicao();
        expedicao.setId(this.id);
        expedicao.setPosicao(this.posicao);
        if (this.pedidoResponseDTO != null) {
            expedicao.setPedido(this.pedidoResponseDTO.toEntity());
        }
        return expedicao;
    }
}
