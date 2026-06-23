package com.tecdes.smart.app_smart_40.service.sse.producer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;

/** Produtor read-only do status da estação PROCESSO (DB2: opByte 4, flagsByte 6). */
@Component
public class ProcessoStatusProducer extends EstacaoStatusProducerBase {

    public ProcessoStatusProducer(PlcConnectionService plcConnectionService,
            ApplicationEventPublisher publisher, ClpIpRegistry ipRegistry) {
        super(plcConnectionService, publisher, ipRegistry, EstacaoClp.PROCESSO, 2, 9, 4, 6);
    }
}
