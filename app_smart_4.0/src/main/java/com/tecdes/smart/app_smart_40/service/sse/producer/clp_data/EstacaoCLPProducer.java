package com.tecdes.smart.app_smart_40.service.sse.producer.clp_data;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    



    @Scheduled(fixedDelayString ="${clp.poll.interval:1000}" )
    public void poll(){
        for(EstacoesCLP e : EstacoesCLP.values()){
            
        }
    }
}
