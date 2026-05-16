package com.tecdes.smart.app_smart_40.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.tecdes.smart.app_smart_40.model.Expedicao;

import lombok.Builder;

@Builder
public record ExpedicaoDTO(
        Long id,
        Integer posicao,
        PedidoDTO pedidoDTO,
        // ADICIONADO: timestamp de entrada na expedição (exigido pelas regras de
        // negócio)
        LocalDateTime dataEntradaExpedicao,
        List<PedidoDTO> pedidos) {

    public static ExpedicaoDTO fromEntity(Expedicao expedicao) {
        return new ExpedicaoDTO(
                expedicao.getId(),
                expedicao.getPosicao(),
                expedicao.getPedido() != null ? PedidoDTO.fromEntity(expedicao.getPedido()) : null,
                expedicao.getDataEntradaExpedicao(),
                // CORRIGIDO: Expedicao não tem mais lista de pedidos (removido da entidade)
                List.of());
    }

}