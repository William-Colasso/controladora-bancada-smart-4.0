package com.tecdes.smart.app_smart_40.service.sse.producer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.ClpLeituraRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;

/** Produtor read-only do status da estação EXPEDIÇÃO (DB9: opByte 32, flagsByte 34). */
@Component
public class ExpedicaoStatusProducer extends EstacaoStatusProducerBase {

    public ExpedicaoStatusProducer(PlcConnectionService plcConnectionService,
            ApplicationEventPublisher publisher, ClpIpRegistry ipRegistry,
            ClpLeituraRegistry leituraRegistry) {
        super(plcConnectionService, publisher, ipRegistry, leituraRegistry, EstacaoClp.EXPEDICAO, 9, 48, 32, 34);
    }
}
