package com.tecdes.smart.app_smart_40.service.clp;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Driver automático do handshake de escrita do CLP (lado <b>escrita</b> do loop CLP↔Backend↔Frontend).
 *
 * <p>Um <b>tick-base</b> ({@code clp.processar.base}, default 100ms) percorre as 4 estações, mas cada
 * uma só é <b>despachada</b> quando o <b>seu</b> intervalo de polling vence — um timer físico controlando
 * N timers lógicos ({@link ClpPollingRegistry}, configurável por estação em runtime). Cada passada roda
 * de forma <b>assíncrona</b> ({@link ClpComandoService#processarAsync}), numa thread do executor, então
 * uma estação lenta/bloqueada no I/O S7 não atrasa as outras. É o que preenche os beans {@code *CLP} e,
 * por consequência, alimenta o SSE {@code estacao-all}.
 *
 * <p><b>Sem empilhar passadas:</b> o scheduler guarda o {@code CompletableFuture} da passada em voo de
 * cada estação e não despacha outra enquanto a anterior não terminar — mesmo que o intervalo já tenha
 * vencido. O tick roda numa única thread do scheduler, então os mapas de controle não precisam de trava.
 *
 * <p><b>Gating por cliente SSE</b> (igual aos produtores read-only): só processa enquanto há ao menos
 * um {@code EventSource} aberto ({@link SseEmitterRegistry#count()} &gt; 0) — ou algo na fila de pedidos
 * a reconciliar. Abrir qualquer tela (home/estacoes/dashboard) liga o processamento; fechar a última aba
 * para — assim não escreve no CLP quando ninguém está olhando.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClpProcessamentoScheduler {

    private final ClpComandoService clpComandoService;
    private final SseEmitterRegistry sseRegistry;
    private final PedidoConsumerList pedidoConsumerList;
    private final ClpPollingRegistry pollingRegistry;

    /** Instante (epoch ms) em que cada estação está liberada para a próxima passada. */
    private final Map<EstacoesCLP, Long> proximaExecucao = new EnumMap<>(EstacoesCLP.class);

    /** Passada assíncrona em voo por estação — evita despachar outra na mesma estação antes de terminar. */
    private final Map<EstacoesCLP, CompletableFuture<?>> passadaEmVoo = new EnumMap<>(EstacoesCLP.class);

    @Scheduled(fixedDelayString = "${clp.processar.base:100}")
    public void processar() {
        // Processa se há alguém olhando (SSE) OU se há pedido na fila a reconciliar
        // (senão a fila nunca lê o magazine do CLP p/ concluir sem nenhuma tela aberta).
        if (sseRegistry.count() == 0 && pedidoConsumerList.filaAtual().isEmpty()) {
            return;
        }
        long agora = System.currentTimeMillis();
        for (EstacoesCLP estacao : EstacoesCLP.values()) {
            Long proxima = proximaExecucao.get(estacao);
            if (proxima != null && agora < proxima) {
                continue; // o intervalo de polling desta estação ainda não venceu
            }
            CompletableFuture<?> emVoo = passadaEmVoo.get(estacao);
            if (emVoo != null && !emVoo.isDone()) {
                continue; // passada anterior desta estação ainda rodando → não empilha outra
            }
            // Despacha assíncrono (não bloqueia o tick nem as outras estações) e reagenda pelo intervalo.
            passadaEmVoo.put(estacao, clpComandoService.processarAsync(estacao));
            proximaExecucao.put(estacao, agora + pollingRegistry.getIntervalo(estacao));
        }
    }
}
