package com.tecdes.smart.app_smart_40.service.sse.producer.clp_data;

import org.springframework.context.ApplicationEventPublisher;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoAllData;
import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;

import lombok.extern.slf4j.Slf4j;

/**
 * Produtor SSE do snapshot completo de uma estação (evento {@code estacao-all} = bean {@code *CLP}).
 *
 * <p>Base abstrata: cada estação tem a <b>sua própria subclasse</b> {@code @Component} com um
 * {@code @Scheduled} próprio, publicando no <b>seu</b> intervalo — estoque/expedicao a cada 300ms,
 * processo/montagem a cada 1s. A subclasse injeta o seu bean {@code *CLP} concreto e chama
 * {@link #publicar()} dentro do seu {@code poll()}.
 */
@Slf4j
public abstract class EstacaoCLPProducer {

    private final ApplicationEventPublisher publisher;
    private final EstacoesCLP estacao;
    private final EstacaoCLP dados;

    protected EstacaoCLPProducer(ApplicationEventPublisher publisher, EstacoesCLP estacao, EstacaoCLP dados) {
        this.publisher = publisher;
        this.estacao = estacao;
        this.dados = dados;
    }

    /** Publica o snapshot atual do bean {@code *CLP} desta estação no barramento (→ SSE {@code estacao-all}). */
    protected void publicar() {
        try {
            publisher.publishEvent(new EstacaoAllData(estacao.getFrontKey(), dados));
        } catch (Exception e) {
            log.error("[CLP {}] falha ao publicar estacao-all: {}", estacao.apiName(), e.getMessage());
        }
    }
}
