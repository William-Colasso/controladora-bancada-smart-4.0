package com.tecdes.smart.app_smart_40.service.sse.producer.status;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

/** Produtor read-only do status da estação PROCESSO (DB2: opByte 4, flagsByte 6). */
@Component
public class ProcessoStatusProducer extends EstacaoStatusProducerBase {

    public ProcessoStatusProducer(PlcConnectionService plcConnectionService,
            ApplicationEventPublisher publisher, ClpIpRegistry ipRegistry,
            SseEmitterRegistry sseRegistry) {
        super(plcConnectionService, publisher, ipRegistry, sseRegistry, EstacoesCLP.PROCESSO, 2, 9, 4, 6);
    }
}
