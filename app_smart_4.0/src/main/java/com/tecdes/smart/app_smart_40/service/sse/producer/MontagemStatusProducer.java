package com.tecdes.smart.app_smart_40.service.sse.producer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.ClpLeituraRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;

/** Produtor read-only do status da estação MONTAGEM (DB57: opByte 4, flagsByte 6). */
@Component
public class MontagemStatusProducer extends EstacaoStatusProducerBase {

    public MontagemStatusProducer(PlcConnectionService plcConnectionService,
            ApplicationEventPublisher publisher, ClpIpRegistry ipRegistry,
            ClpLeituraRegistry leituraRegistry) {
        super(plcConnectionService, publisher, ipRegistry, leituraRegistry, EstacaoClp.MONTAGEM, 57, 9, 4, 6);
    }
}
