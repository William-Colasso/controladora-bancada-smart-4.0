package com.tecdes.smart.app_smart_40.service.sse.producer.status;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.EstoqueCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

/** Produtor do status da estação ESTOQUE, derivado do bean {@link EstoqueCLP} (sem ler o socket). */
@Component
public class EstoqueStatusProducer extends EstacaoStatusProducerBase {

    public EstoqueStatusProducer(ApplicationEventPublisher publisher, SseEmitterRegistry sseRegistry,
            EstadoProducaoService estado, EstoqueCLP dados) {
        super(publisher, sseRegistry, estado, dados, EstacoesCLP.ESTOQUE);
    }
}
