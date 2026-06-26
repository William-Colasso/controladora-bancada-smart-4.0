package com.tecdes.smart.app_smart_40.service.sse.producer.clp_data;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.MontagemCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;

import tools.jackson.databind.ObjectMapper;

/** Publica o snapshot completo da estação MONTAGEM a cada 1s (evento {@code estacao-all}), on-change. */
@Component
public class MontagemCLPProducer extends EstacaoCLPProducer {

    public MontagemCLPProducer(ApplicationEventPublisher publisher, EstadoProducaoService estado,
            MontagemCLP dados, ObjectMapper mapper) {
        super(publisher, estado, EstacoesCLP.MONTAGEM, dados, mapper);
    }

    @Scheduled(fixedDelayString = "${clp.poll.montagem:1000}")
    public void poll() {
        publicar();
    }
}
