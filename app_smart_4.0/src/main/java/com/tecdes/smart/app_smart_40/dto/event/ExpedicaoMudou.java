package com.tecdes.smart.app_smart_40.dto.event;

/**
 * Marcador: a expedição (banco) sofreu uma mutação — inclui mudança de status do pedido vinculado,
 * já que o grid exibe o pedido. Publicado pelos métodos de escrita do {@code ExpedicaoService} (e
 * {@code PedidoService.concluir}); o {@code ClpEventoCoordinator} escuta, re-consulta o grid e
 * publica {@link ExpedicaoGridEvent} só se o conteúdo de fato mudou.
 */
public record ExpedicaoMudou() {
}
