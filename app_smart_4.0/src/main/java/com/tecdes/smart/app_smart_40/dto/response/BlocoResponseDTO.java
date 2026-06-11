package com.tecdes.smart.app_smart_40.dto.response;

import java.util.List;

import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

public record BlocoResponseDTO(Long id, CorBloco cor, List<LaminaResponseDTO> laminas) {

    public static BlocoResponseDTO fromEntity(Bloco bloco) {
        return new BlocoResponseDTO(
                bloco.getId(),
                bloco.getCor(),
                bloco.getLaminas().stream().map(LaminaResponseDTO::fromEntity).toList());
    }

    public static Bloco toEntity(BlocoResponseDTO blocoDTO) {
        return Bloco.builder()
                .id(blocoDTO.id())
                .cor(blocoDTO.cor())
                .laminas(blocoDTO.laminas().stream().map(LaminaResponseDTO::toEntity).toList())
                .build();
    }
}
