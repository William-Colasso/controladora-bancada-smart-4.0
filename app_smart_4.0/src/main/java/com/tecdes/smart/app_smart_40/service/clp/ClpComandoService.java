package com.tecdes.smart.app_smart_40.service.clp;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
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

    private final Map<EstacaoClp, EstacaoClpHandshake> handshakes = new EnumMap<>(EstacaoClp.class);
    private final ClpIpRegistry ipRegistry;

    public ClpComandoService(List<EstacaoClpHandshake> handshakes, ClpIpRegistry ipRegistry) {
        for (EstacaoClpHandshake h : handshakes) {
            this.handshakes.put(h.estacao(), h);
        }
        this.ipRegistry = ipRegistry;
    }

    /** Executa uma passada de leitura+escrita na estação. IP não configurado → estado inalterado. */
    public void processar(EstacaoClp estacao) {
        EstacaoClpHandshake handshake = handshakes.get(estacao);
        if (handshake == null) {
            throw new IllegalStateException("Estação sem serviço de handshake: " + estacao.apiName());
        }
        String ip = ipRegistry.getIp(estacao);
        if (ip == null || ip.isBlank()) {
            log.warn("Estação {} sem IP configurado — handshake ignorado.", estacao.apiName());
            return;
        }
        handshake.lerEProcessar(ip);
    }

    /** Executa uma passada em todas as estações. */
    public void processarTodas() {
        for (EstacaoClp estacao : EstacaoClp.values()) {
            processar(estacao);
        }
    }
}
