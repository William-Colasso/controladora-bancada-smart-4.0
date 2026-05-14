package com.tecdes.smart.app_smart_40.dto;

import java.util.List;

import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

public record BlocoDTO(
    Long idPedido,
    Long idEstoque,
    CorBloco cor,
    List<LaminaDTO> laminas
) {}
