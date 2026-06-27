package com.tecdes.smart.app_smart_40.service.sse.producer.status;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.ProcessoCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

/** Produtor do status da estação PROCESSO, derivado do bean {@link ProcessoCLP} (sem ler o socket). */
@Component
public class ProcessoStatusProducer extends EstacaoStatusProducerBase {

    public ProcessoStatusProducer(ApplicationEventPublisher publisher, SseEmitterRegistry sseRegistry,
            EstadoProducaoService estado, ProcessoCLP dados) {
        super(publisher, sseRegistry, estado, dados, EstacoesCLP.PROCESSO);
    }
}
