package com.tecdes.smart.app_smart_40.dto.event;

/**
 * Evento (contrato de dados, sem comportamento) com o status em tempo real de uma estação do CLP.
 *
 * <p>Publicado pelos produtores read-only em {@code service/sse/producer} e roteado por tipo pelo
 * {@code ApplicationEventPublisher} até o {@code SseNotifier}. Nenhum lado conhece o outro.
 *
 * @param estacao       chave da estação ({@code estoque}, {@code producao}, {@code montagem},
 *                      {@code expedicao}) — casa com {@code bancadaStatus.js#ESTACOES}.
 * @param estado        camada de estado: {@code off}, {@code on} ou {@code pause}.
 * @param funcionamento camada de ocupação: {@code 0}, {@code 1}, {@code 2} ou {@code null} (oculta).
 */
public record EstacaoStatusEvent(String estacao, String estado, Integer funcionamento) {
}
