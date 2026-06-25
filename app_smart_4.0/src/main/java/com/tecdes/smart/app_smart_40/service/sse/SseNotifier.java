package com.tecdes.smart.app_smart_40.service.sse;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoAllData;
import com.tecdes.smart.app_smart_40.dto.event.EstacaoStatusEvent;
import com.tecdes.smart.app_smart_40.dto.event.EstoqueGridEvent;
import com.tecdes.smart.app_smart_40.dto.event.ExpedicaoGridEvent;

import lombok.RequiredArgsConstructor;

/**
 * Notificador SSE: único listener que sabe traduzir um evento do barramento numa mensagem do canal.
 *
 * <p>Um {@code @EventListener} por tipo de evento (o tipo é o contrato de roteamento — não há strings
 * mágicas nem referência direta às fontes). Sem lógica de comparação: o filtro "publicar ou não" vive
 * no produtor. {@code @Async} garante que o broadcast não bloqueie a thread do {@code @Scheduled} que
 * publicou o evento.
 *
 * <p>Adicionar uma fonte nova depois = um novo {@code @EventListener} aqui; nada existente é tocado.
 */
@Component
@RequiredArgsConstructor
public class SseNotifier {

    private final SseEmitterRegistry registry;

    @Async
    @EventListener
    public void onEstacaoStatus(EstacaoStatusEvent evento) {
        // estacao() distingue as 4 estações que compartilham o nome de evento (cache de replay).
        registry.broadcast("estacao-status", evento.estacao(), evento);
    }

    @Async
    @EventListener
    public void onEstacaoAll(EstacaoAllData evento) {
        registry.broadcast("estacao-all", evento.estacao(), evento);
    }

    @Async
    @EventListener
    public void onEstoqueGrid(EstoqueGridEvent evento) {
        registry.broadcast("estoque", evento);
    }

    @Async
    @EventListener
    public void onExpedicaoGrid(ExpedicaoGridEvent evento) {
        registry.broadcast("expedicao", evento);
    }
}
