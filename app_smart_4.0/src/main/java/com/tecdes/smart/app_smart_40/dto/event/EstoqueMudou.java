package com.tecdes.smart.app_smart_40.dto.event;

/**
 * Marcador: o estoque (banco) sofreu uma mutação. Publicado pelos métodos de escrita do
 * {@code EstoqueService}; o {@code ClpEventoCoordinator} escuta, re-consulta o grid e publica
 * {@link EstoqueGridEvent} só se o conteúdo de fato mudou.
 */
public record EstoqueMudou() {
}
