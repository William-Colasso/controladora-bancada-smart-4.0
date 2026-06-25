package com.tecdes.smart.app_smart_40.service.sse.producer.status;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

/** Produtor read-only do status da estação ESTOQUE (DB9: opByte 98, flagsByte 100). */
@Component
public class EstoqueStatusProducer extends EstacaoStatusProducerBase {

    public EstoqueStatusProducer(PlcConnectionService plcConnectionService,
            ApplicationEventPublisher publisher, ClpIpRegistry ipRegistry,
            SseEmitterRegistry sseRegistry) {
        super(plcConnectionService, publisher, ipRegistry, sseRegistry, EstacaoClp.ESTOQUE, 9, 111, 98, 100);
    }
}
