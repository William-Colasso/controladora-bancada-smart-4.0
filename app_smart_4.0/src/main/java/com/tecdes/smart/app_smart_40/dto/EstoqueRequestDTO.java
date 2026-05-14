package com.tecdes.smart.app_smart_40.dto;

import com.tecdes.smart.app_smart_40.model.enums.CorBloco;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
public record EstoqueRequestDTO(Byte posicao,CorBloco corBloco) {

}