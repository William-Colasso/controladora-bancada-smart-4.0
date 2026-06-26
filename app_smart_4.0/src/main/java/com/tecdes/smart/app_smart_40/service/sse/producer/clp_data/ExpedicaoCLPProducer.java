package com.tecdes.smart.app_smart_40.service.sse.producer.clp_data;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.ExpedicaoCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;

/** Publica o snapshot completo da estação EXPEDICAO a cada 300ms (evento {@code estacao-all}). */
@Component
public class ExpedicaoCLPProducer extends EstacaoCLPProducer {

    public ExpedicaoCLPProducer(ApplicationEventPublisher publisher, EstadoProducaoService estado,
            ExpedicaoCLP dados) {
        super(publisher, estado, EstacoesCLP.EXPEDICAO, dados);
    }

    @Scheduled(fixedDelayString = "${clp.poll.expedicao:300}")
    public void poll() {
        publicar();
    }
}
