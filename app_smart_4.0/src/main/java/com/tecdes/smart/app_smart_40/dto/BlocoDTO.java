package com.tecdes.smart.app_smart_40.dto;

import java.util.List;

public record BlocoDTO(
    Long idPedido,
    Long idEstoque,
    Integer cor,
    List<LaminaDTO> laminas
) {}
