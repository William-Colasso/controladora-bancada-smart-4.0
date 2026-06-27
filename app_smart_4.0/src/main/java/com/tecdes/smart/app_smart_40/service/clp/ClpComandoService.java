package com.tecdes.smart.app_smart_40.service.clp;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoHeartbeat;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.estacao.EstacaoClpHandshake;

import lombok.extern.slf4j.Slf4j;

/**
 * Disparo sob demanda do handshake de escrita com o CLP (lado <b>escrita</b> do loop CLP↔Backend↔Frontend).
 *
 * <p>Despacha uma passada de {@link EstacaoClpHandshake#lerEProcessar(String)} para a estação certa,
 * resolvendo o IP no {@link ClpIpRegistry} no momento da chamada — mesma fonte de IP da leitura SSE,
 * então a troca via {@code PUT /api/clp/ips/{estacao}} vale na hora. <b>Sem agendamento:</b> cada chamada
 * é uma passada; quem repete é o caller (REST {@code POST /api/clp/{estacao}/processar}).
 */
@Service
@Slf4j
public class ClpComandoService {

    private final Map<EstacoesCLP, EstacaoClpHandshake> handshakes = new EnumMap<>(EstacoesCLP.class);
    private final ClpIpRegistry ipRegistry;
    private final ApplicationEventPublisher publisher;

    public ClpComandoService(List<EstacaoClpHandshake> handshakes, ClpIpRegistry ipRegistry,
            ApplicationEventPublisher publisher) {
        for (EstacaoClpHandshake h : handshakes) {
            this.handshakes.put(h.estacao(), h);
        }
        this.ipRegistry = ipRegistry;
        this.publisher = publisher;
    }

    /** Executa uma passada de leitura+escrita na estação. IP não configurado → estado inalterado. */
    public void processar(EstacoesCLP estacao) {
        EstacaoClpHandshake handshake = handshakes.get(estacao);
        if (handshake == null) {
            throw new IllegalStateException("Estação sem serviço de handshake: " + estacao.apiName());
        }
        String ip = ipRegistry.getIp(estacao);
        if (ip == null || ip.isBlank()) {
            log.warn("Estação {} sem IP configurado — handshake ignorado.", estacao.apiName());
            return;
        }
        boolean ok = handshake.lerEProcessar(ip);
        // O dado completo (estacao-all) sai dos producers (clp_data) a partir do bean *CLP já preenchido
        // por esta passada. Aqui o write path só pulsa o heartbeat de "leitura viva" quando a leitura
        // ocorreu de fato — é o sinal de liveness que o front usa (badge home + watchdog estações).
        if (ok) {
            publisher.publishEvent(new EstacaoHeartbeat(estacao.getFrontKey()));
        }
    }

    /** Executa uma passada em todas as estações. */
    
    
    public void processarTodas() {
        for (EstacoesCLP estacao : EstacoesCLP.values()) {
            processar(estacao);
        }
    }
}
