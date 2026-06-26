package com.tecdes.smart.app_smart_40.service.sse.producer.clp_data;

import org.springframework.context.ApplicationEventPublisher;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoAllData;
import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;

import lombok.extern.slf4j.Slf4j;

/**
 * Produtor SSE do snapshot completo de uma estação (evento {@code estacao-all} = bean {@code *CLP}).
 *
 * <p>Base abstrata: cada estação tem a <b>sua própria subclasse</b> {@code @Component} com um
 * {@code @Scheduled} próprio, publicando no <b>seu</b> intervalo — estoque/expedicao a cada 300ms,
 * processo/montagem a cada 1s. A subclasse injeta o seu bean {@code *CLP} concreto e chama
 * {@link #publicar()} dentro do seu {@code poll()}.
 *
 * <p><b>Gate de frescor:</b> o bean só é preenchido pelo caminho de escrita ({@code processData}).
 * Para não transmitir dados velhos (tudo {@code false/null/0}) quando a comunicação está parada, só
 * publica enquanto houve leitura nos últimos {@link #FRESCOR_MS} ms ({@link EstadoProducaoService#getUltimoLeituraMillis()}).
 */
@Slf4j
public abstract class EstacaoCLPProducer {

    /** Janela de frescor: sem leitura há mais que isso, o produtor para de publicar (comunicação parada). */
    private static final long FRESCOR_MS = 1500;

    private final ApplicationEventPublisher publisher;
    private final EstadoProducaoService estado;
    private final EstacoesCLP estacao;
    private final EstacaoCLP dados;

    protected EstacaoCLPProducer(ApplicationEventPublisher publisher, EstadoProducaoService estado,
            EstacoesCLP estacao, EstacaoCLP dados) {
        this.publisher = publisher;
        this.estado = estado;
        this.estacao = estacao;
        this.dados = dados;
    }

    /**
     * Publica o snapshot atual do bean {@code *CLP} desta estação no barramento (→ SSE {@code estacao-all}),
     * desde que a leitura esteja recente (gate de frescor) — senão a comunicação está parada e nada é emitido.
     */
    protected void publicar() {
        if (System.currentTimeMillis() - estado.getUltimoLeituraMillis() > FRESCOR_MS) {
            return; // comunicação parada → não republica bean velho
        }
        try {
            publisher.publishEvent(new EstacaoAllData(estacao.getFrontKey(), dados));
        } catch (Exception e) {
            log.error("[CLP {}] falha ao publicar estacao-all: {}", estacao.apiName(), e.getMessage());
        }
    }
}
