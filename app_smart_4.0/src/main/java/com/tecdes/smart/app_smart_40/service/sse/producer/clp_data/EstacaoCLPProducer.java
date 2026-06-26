package com.tecdes.smart.app_smart_40.service.sse.producer.clp_data;

import org.springframework.context.ApplicationEventPublisher;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoAllData;
import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Produtor SSE do snapshot completo de uma estação (evento {@code estacao-all} = bean {@code *CLP}).
 *
 * <p>Base abstrata: cada estação tem a <b>sua própria subclasse</b> {@code @Component} com um
 * {@code @Scheduled} próprio, publicando no <b>seu</b> intervalo — estoque/expedicao a cada 300ms,
 * processo/montagem a cada 1s. A subclasse injeta o seu bean {@code *CLP} concreto e chama
 * {@link #publicar()} dentro do seu {@code poll()}. <b>Nunca lê o socket</b> — só o bean, que é
 * preenchido pelo write path ({@code *ClpService.processData}).
 *
 * <p><b>Gate de frescor:</b> o bean só é preenchido pelo caminho de escrita. Para não transmitir dados
 * velhos quando a comunicação está parada, só publica enquanto houve leitura nos últimos
 * {@link #FRESCOR_MS} ms ({@link EstadoProducaoService#getUltimoLeituraMillis()}).
 *
 * <p><b>On-change:</b> publica só quando o bean muda. Como o producer injeta o <b>mesmo singleton</b>
 * {@code *CLP} que o write path muta, comparar a referência consigo mesma daria sempre "igual"; por
 * isso a detecção compara a <b>assinatura JSON</b> serializada (lida arrays como
 * {@code posicoesOcupadas[]}/{@code orderExpedicao[]} sem {@code equals()} por bean).
 */
@Slf4j
public abstract class EstacaoCLPProducer {

    /** Janela de frescor: sem leitura há mais que isso, o produtor para de publicar (comunicação parada). */
    private static final long FRESCOR_MS = 1500;

    private final ApplicationEventPublisher publisher;
    private final EstadoProducaoService estado;
    private final EstacoesCLP estacao;
    private final EstacaoCLP dados;
    private final ObjectMapper mapper;

    /** Assinatura JSON da última emissão (o bean é singleton mutável → comparar valor serializado). */
    private String ultimoJson;

    protected EstacaoCLPProducer(ApplicationEventPublisher publisher, EstadoProducaoService estado,
            EstacoesCLP estacao, EstacaoCLP dados, ObjectMapper mapper) {
        this.publisher = publisher;
        this.estado = estado;
        this.estacao = estacao;
        this.dados = dados;
        this.mapper = mapper;
    }

    /**
     * Publica o snapshot do bean {@code *CLP} desta estação (→ SSE {@code estacao-all}) quando ele muda,
     * desde que a leitura esteja recente (gate de frescor) — senão a comunicação está parada e nada é emitido.
     */
    protected void publicar() {
        if (System.currentTimeMillis() - estado.getUltimoLeituraMillis() > FRESCOR_MS) {
            return; // comunicação parada → não republica bean velho
        }
        try {
            String json = mapper.writeValueAsString(dados);
            if (json.equals(ultimoJson)) {
                return; // sem mudança → não republica
            }
            ultimoJson = json;
            publisher.publishEvent(new EstacaoAllData(estacao.getFrontKey(), dados));
        } catch (Exception e) {
            log.error("[CLP {}] falha ao publicar estacao-all: {}", estacao.apiName(), e.getMessage());
        }
    }
}
