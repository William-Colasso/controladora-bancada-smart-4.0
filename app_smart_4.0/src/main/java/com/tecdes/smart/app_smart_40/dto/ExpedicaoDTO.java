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
        List<PedidoDTO> pedidos) {

    public static ExpedicaoDTO fromEntity(Expedicao expedicao) {
        return new ExpedicaoDTO(
                expedicao.getId(),
                expedicao.getPosicao(),
                expedicao.getPedido() != null ? PedidoDTO.fromEntity(expedicao.getPedido()) : null,
                List.of());
    }

    public static Expedicao toEntity(ExpedicaoDTO dto) {
        Expedicao expedicao = new Expedicao();
        expedicao.setId(dto.id());
        expedicao.setPosicao(dto.posicao());
        if (dto.pedidoDTO() != null) {
            expedicao.setPedido(dto.pedidoDTO().toEntity());
        }
        return expedicao;
    }

}