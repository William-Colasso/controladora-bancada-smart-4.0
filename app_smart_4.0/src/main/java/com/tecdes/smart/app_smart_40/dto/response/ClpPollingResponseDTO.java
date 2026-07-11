package com.tecdes.smart.app_smart_40.dto.response;

/** Intervalo de polling (ms) configurado do CLP de uma estação (estoque/processo/montagem/expedicao). */
public record ClpPollingResponseDTO(String estacao, Long intervaloMs) {
}
