package com.tecdes.smart.app_smart_40.service.clp;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;

import lombok.extern.slf4j.Slf4j;

/**
 * Flag de leitura por estação — controla quais estações os produtores read-only do SSE devem ler.
 *
 * <p>Default no boot: <b>todas desabilitadas</b> (nada lê o CLP até o usuário conectar). O frontend
 * habilita uma estação ao clicar "Conectar" ({@code POST /api/clp/{estacao}/conectar}) e desabilita
 * ao "Desconectar". Os produtores ({@link com.tecdes.smart.app_smart_40.service.sse.producer}) consultam
 * {@link #isHabilitada} a cada ciclo, então a troca entra em vigor sem reiniciar.
 *
 * <p>Thread-safe: produtores (threads do scheduler) leem; a API escreve. Mesma estética do
 * {@link ClpIpRegistry}.
 */
@Component
@Slf4j
public class ClpLeituraRegistry {

    private final Map<EstacaoClp, Boolean> habilitadas = new EnumMap<>(EstacaoClp.class);

    public synchronized boolean isHabilitada(EstacaoClp estacao) {
        return Boolean.TRUE.equals(habilitadas.get(estacao));
    }

    public synchronized void habilitar(EstacaoClp estacao) {
        habilitadas.put(estacao, Boolean.TRUE);
        log.info("Leitura da estação {} habilitada.", estacao.apiName());
    }

    public synchronized void desabilitar(EstacaoClp estacao) {
        habilitadas.put(estacao, Boolean.FALSE);
        log.info("Leitura da estação {} desabilitada.", estacao.apiName());
    }

    /** Cópia do mapa (ordem do enum) para exposição/diagnóstico. */
    public synchronized Map<EstacaoClp, Boolean> snapshot() {
        Map<EstacaoClp, Boolean> copia = new EnumMap<>(EstacaoClp.class);
        for (EstacaoClp e : EstacaoClp.values()) {
            copia.put(e, isHabilitada(e));
        }
        return copia;
    }
}
