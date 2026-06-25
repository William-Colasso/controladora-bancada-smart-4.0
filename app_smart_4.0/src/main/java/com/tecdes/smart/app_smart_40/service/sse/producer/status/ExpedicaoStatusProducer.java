package com.tecdes.smart.app_smart_40.service.sse.producer.status;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

/** Produtor read-only do status da estação EXPEDIÇÃO (DB9: opByte 32, flagsByte 34). */
@Component
public class ExpedicaoStatusProducer extends EstacaoStatusProducerBase {

    public ExpedicaoStatusProducer(PlcConnectionService plcConnectionService,
            ApplicationEventPublisher publisher, ClpIpRegistry ipRegistry,
            SseEmitterRegistry sseRegistry) {
        super(plcConnectionService, publisher, ipRegistry, sseRegistry, EstacoesCLP.EXPEDICAO, 9, 48, 32, 34);
    }
}
