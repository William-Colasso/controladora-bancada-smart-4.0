package com.tecdes.smart.app_smart_40.service.sse.producer.status;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

/** Produtor read-only do status da estação MONTAGEM (DB57: opByte 4, flagsByte 6). */
@Component
public class MontagemStatusProducer extends EstacaoStatusProducerBase {

    public MontagemStatusProducer(PlcConnectionService plcConnectionService,
            ApplicationEventPublisher publisher, ClpIpRegistry ipRegistry,
            SseEmitterRegistry sseRegistry) {
        super(plcConnectionService, publisher, ipRegistry, sseRegistry, EstacoesCLP.MONTAGEM, 57, 9, 4, 6);
    }
}
