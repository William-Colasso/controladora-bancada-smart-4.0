package com.tecdes.smart.app_smart_40.service.sse.producer.clp_data;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.clp.EstoqueCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;

/** Publica o snapshot completo da estação ESTOQUE a cada 300ms (evento {@code estacao-all}). */
@Component
public class EstoqueCLPProducer extends EstacaoCLPProducer {

    public EstoqueCLPProducer(ApplicationEventPublisher publisher, EstoqueCLP dados) {
        super(publisher, EstacoesCLP.ESTOQUE, dados);
    }

    @Scheduled(fixedDelayString = "${clp.poll.estoque:300}")
    public void poll() {
        publicar();
    }
}
