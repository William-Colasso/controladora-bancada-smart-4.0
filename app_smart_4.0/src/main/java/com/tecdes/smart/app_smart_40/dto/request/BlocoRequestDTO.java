package com.tecdes.smart.app_smart_40.dto.request;

import java.util.List;

import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.enums.AndarBloco;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

public record BlocoRequestDTO(CorBloco cor, AndarBloco andar, List<LaminaRequestDTO> laminas) {

    public static BlocoRequestDTO fromEntity(Bloco bloco) {
        return new BlocoRequestDTO(
                bloco.getCor(),
                bloco.getAndar(),
                bloco.getLaminas().stream()
                        .map(l -> new LaminaRequestDTO(l.getCor(), l.getPadrao(), l.getPosicao())).toList());
    }

    public static Bloco toEntity(BlocoRequestDTO blocoDTO) {
        return Bloco.builder()
                .cor(blocoDTO.cor)
                .andar(blocoDTO.andar())
                .laminas(blocoDTO.laminas.stream().map(LaminaRequestDTO::toEntity).toList())
                .build();
    }
}
