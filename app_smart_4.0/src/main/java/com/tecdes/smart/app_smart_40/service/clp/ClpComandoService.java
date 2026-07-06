package com.tecdes.smart.app_smart_40.service.clp;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.estacao.EstacaoClpHandshake;
import com.tecdes.smart.app_smart_40.service.sse.ClpEventoCoordinator;

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
    private final ClpEventoCoordinator coordinator;

    public ClpComandoService(List<EstacaoClpHandshake> handshakes, ClpIpRegistry ipRegistry,
            ClpEventoCoordinator coordinator) {
        for (EstacaoClpHandshake h : handshakes) {
            this.handshakes.put(h.estacao(), h);
        }
        this.ipRegistry = ipRegistry;
        this.coordinator = coordinator;
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
        // Passada lida → o coordenador decide o que publicar: heartbeat sempre (liveness),
        // estacao-all/estacao-status só se as variáveis do bean mudaram desde a última emissão.
        if (ok) {
            coordinator.aoPassadaLida(estacao, handshake.dados());
        }
    }

    /** Executa uma passada em todas as estações. */
    
    
    public void processarTodas() {
        for (EstacoesCLP estacao : EstacoesCLP.values()) {
            processar(estacao);
        }
    }
}
