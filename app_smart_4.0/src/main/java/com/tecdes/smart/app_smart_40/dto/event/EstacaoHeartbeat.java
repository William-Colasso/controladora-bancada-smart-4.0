package com.tecdes.smart.app_smart_40.dto.event;

/** Pulso de "leitura viva" de uma estação: emitido a cada passada lida com sucesso (write path). */
public record EstacaoHeartbeat(String estacao) {
}
