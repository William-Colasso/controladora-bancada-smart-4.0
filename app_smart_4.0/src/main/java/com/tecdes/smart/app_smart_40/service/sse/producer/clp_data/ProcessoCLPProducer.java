package com.tecdes.smart.app_smart_40.service.sse.producer.clp_data;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.ProcessoCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;

import tools.jackson.databind.ObjectMapper;

/** Publica o snapshot completo da estação PROCESSO a cada 1s (evento {@code estacao-all}), on-change. */
@Component
public class ProcessoCLPProducer extends EstacaoCLPProducer {

    public ProcessoCLPProducer(ApplicationEventPublisher publisher, EstadoProducaoService estado,
            ProcessoCLP dados, ObjectMapper mapper) {
        super(publisher, estado, EstacoesCLP.PROCESSO, dados, mapper);
    }

    @Scheduled(fixedDelayString = "${clp.poll.processo:1000}")
    public void poll() {
        publicar();
    }
}
