package com.tecdes.smart.app_smart_40.dto;

import java.util.List;

import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

public record BlocoDTO(
        PedidoDTO pedidoDTO,
        EstoqueDTO estoque,
        CorBloco cor,
        List<LaminaDTO> laminas) {

    public static BlocoDTO fromEntity(Bloco bloco) {
        return new BlocoDTO(
                PedidoDTO.fromEntity(bloco.getPedido()),
                EstoqueDTO.fromEntity(bloco.getEstoque()),
                bloco.getCor(),
                bloco.getLaminas().stream().map(LaminaDTO::fromEntity).toList());
    }

    public static Bloco toEntity(BlocoDTO blocoDTO) {
        return Bloco.builder()
                .pedido(blocoDTO.pedidoDTO.toEntity())
                .estoque(EstoqueDTO.toEntity(blocoDTO.estoque))
                .cor(blocoDTO.cor)
                .laminas(blocoDTO.laminas.stream().map(LaminaDTO::toEntity).toList())
                .build();
    }
}
