package com.tecdes.smart.app_smart_40.service.sse.producer.status;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.MontagemCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

/** Produtor do status da estação MONTAGEM, derivado do bean {@link MontagemCLP} (sem ler o socket). */
@Component
public class MontagemStatusProducer extends EstacaoStatusProducerBase {

    public MontagemStatusProducer(ApplicationEventPublisher publisher, SseEmitterRegistry sseRegistry,
            EstadoProducaoService estado, MontagemCLP dados) {
        super(publisher, sseRegistry, estado, dados, EstacoesCLP.MONTAGEM);
    }
}
