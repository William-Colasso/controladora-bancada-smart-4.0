package com.tecdes.smart.app_smart_40.service.sse.producer.clp_data;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.clp.ProcessoCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;

/** Publica o snapshot completo da estação PROCESSO a cada 1s (evento {@code estacao-all}). */
@Component
public class ProcessoCLPProducer extends EstacaoCLPProducer {

    public ProcessoCLPProducer(ApplicationEventPublisher publisher, ProcessoCLP dados) {
        super(publisher, EstacoesCLP.PROCESSO, dados);
    }

    @Scheduled(fixedDelayString = "${clp.poll.processo:1000}")
    public void poll() {
        publicar();
    }
}
