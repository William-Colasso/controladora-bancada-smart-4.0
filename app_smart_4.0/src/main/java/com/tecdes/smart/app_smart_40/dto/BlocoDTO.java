package com.tecdes.smart.app_smart_40.dto;

import java.util.List;

import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

public record BlocoDTO(
    PedidoDTO pedidoDTO,
    EstoqueDTO estoque,
    CorBloco cor,
    List<LaminaDTO> laminas
) {}
