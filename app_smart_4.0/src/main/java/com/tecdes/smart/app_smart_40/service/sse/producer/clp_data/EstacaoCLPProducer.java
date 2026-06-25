package com.tecdes.smart.app_smart_40.service.sse.producer.clp_data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.emitter.EmitterException;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoAllData;
import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EstacaoCLPProducer {
    private final ApplicationEventPublisher publisher;
    private final SseEmitterRegistry sseRegistry;
    private final ClpIpRegistry ipRegistry;

    private final Map<String, EstacaoCLP> estacoes;

    @Scheduled(fixedDelayString = "${clp.poll.interval:1000}")
    public void poll() {
        for (EstacoesCLP e : EstacoesCLP.values()) {
            String frontKey = e.getFrontKey();
            EstacaoCLP atual = estacoes.get(frontKey);
            try {

                publisher.publishEvent(new EstacaoAllData(frontKey, atual));

            } catch (EmitterException emitterException) {
                log.error(emitterException.getMessage());
            }
            catch( NullPointerException nu){
                log.error(nu.getLocalizedMessage());
                
            }
        }
    }
}
