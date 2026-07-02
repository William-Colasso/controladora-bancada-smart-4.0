package com.tecdes.smart.app_smart_40.service.sse.producer.status;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.ExpedicaoCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

/** Produtor do status da estação EXPEDIÇÃO, derivado do bean {@link ExpedicaoCLP} (sem ler o socket). */
@Component
public class ExpedicaoStatusProducer extends EstacaoStatusProducerBase {

    public ExpedicaoStatusProducer(ApplicationEventPublisher publisher, SseEmitterRegistry sseRegistry,
            EstadoProducaoService estado, ExpedicaoCLP dados) {
        super(publisher, sseRegistry, estado, dados, EstacoesCLP.EXPEDICAO);
    }
}
