package com.tecdes.smart.app_smart_40.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.tecdes.smart.app_smart_40.model.Expedicao;

import lombok.Builder;

@Builder
public record ExpedicaoDTO(
        Long id,
        Integer posicao,
        PedidoDTO pedidoDTO,
        List<PedidoDTO> pedidos) {
    public static ExpedicaoDTO fromEntity(Expedicao expedicao) {
        return new ExpedicaoDTO(
                expedicao.getId(),
                expedicao.getPosicao(),
                PedidoDTO.fromEntity(expedicao.getPedido()),
                expedicao.getPedidos().stream().map(pedido -> PedidoDTO.fromEntity(pedido))
                        .collect(Collectors.toList()));
    }

}