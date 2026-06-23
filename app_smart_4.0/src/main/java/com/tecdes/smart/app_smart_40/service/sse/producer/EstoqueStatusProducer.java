package com.tecdes.smart.app_smart_40.service.sse.producer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.ClpLeituraRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;

/** Produtor read-only do status da estação ESTOQUE (DB9: opByte 98, flagsByte 100). */
@Component
public class EstoqueStatusProducer extends EstacaoStatusProducerBase {

    public EstoqueStatusProducer(PlcConnectionService plcConnectionService,
            ApplicationEventPublisher publisher, ClpIpRegistry ipRegistry,
            ClpLeituraRegistry leituraRegistry) {
        super(plcConnectionService, publisher, ipRegistry, leituraRegistry, EstacaoClp.ESTOQUE, 9, 111, 98, 100);
    }
}
