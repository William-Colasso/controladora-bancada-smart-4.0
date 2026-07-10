package com.tecdes.smart.app_smart_40.service.clp;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Driver automático do handshake de escrita do CLP (lado <b>escrita</b> do loop CLP↔Backend↔Frontend).
 *
 * <p>Roda {@link ClpComandoService#processarTodas()} num intervalo fixo
 * ({@code clp.processar.interval}, default 300ms) — <b>por padrão</b>, sem depender de botão no front.
 * É o que preenche os beans {@code *CLP} e, por consequência, alimenta o SSE {@code estacao-all}.
 *
 * <p><b>Gating por cliente SSE</b> (igual aos produtores read-only): só processa enquanto há ao menos
 * um {@code EventSource} aberto ({@link SseEmitterRegistry#count()} &gt; 0). Abrir qualquer tela
 * (home/estacoes/dashboard) liga o processamento; fechar a última aba para — assim não escreve no CLP
 * quando ninguém está olhando.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClpProcessamentoScheduler {

    private final ClpComandoService clpComandoService;
    private final SseEmitterRegistry sseRegistry;
    private final PedidoConsumerList pedidoConsumerList;

    @Scheduled(fixedDelayString = "${clp.processar.interval:300}")
    public void processar() {
        // Processa se há alguém olhando (SSE) OU se há pedido na fila a reconciliar
        // (senão a fila nunca lê o magazine do CLP p/ concluir sem nenhuma tela aberta).
        if (sseRegistry.count() == 0 && pedidoConsumerList.filaAtual().isEmpty()) {
            return;
        }
        try {
            clpComandoService.processarTodas();
        } catch (Exception e) {
            log.error("Falha no processamento automático do CLP: {}", e.getMessage());
        }
    }
}
