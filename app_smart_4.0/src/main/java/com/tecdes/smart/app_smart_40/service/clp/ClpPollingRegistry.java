package com.tecdes.smart.app_smart_40.service.clp;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;

import lombok.extern.slf4j.Slf4j;

/**
 * Registro mutável, em tempo de execução, do intervalo de polling (ms) do CLP de cada estação.
 *
 * <p>Mesmo padrão do {@link ClpIpRegistry}: semeado no boot pelas propriedades
 * {@code clp.processar.interval.<estacao>} (cada uma com fallback ao default global
 * {@code clp.processar.interval}, 300ms) e alterável via REST ({@code PUT /api/clp/polling/{estacao}}).
 * O {@code ClpProcessamentoScheduler} consulta {@link #getIntervalo} a cada tick, então a troca de
 * intervalo entra em vigor sem reiniciar a aplicação.
 *
 * <p>Thread-safe (métodos {@code synchronized}): o scheduler lê; a API escreve.
 */
@Component
@Slf4j
public class ClpPollingRegistry {

    private final Map<EstacoesCLP, Long> intervalos = new EnumMap<>(EstacoesCLP.class);

    public ClpPollingRegistry(
            @Value("${clp.processar.interval.estoque:${clp.processar.interval:300}}") long estoque,
            @Value("${clp.processar.interval.processo:${clp.processar.interval:300}}") long processo,
            @Value("${clp.processar.interval.montagem:${clp.processar.interval:300}}") long montagem,
            @Value("${clp.processar.interval.expedicao:${clp.processar.interval:300}}") long expedicao) {
        intervalos.put(EstacoesCLP.ESTOQUE, estoque);
        intervalos.put(EstacoesCLP.PROCESSO, processo);
        intervalos.put(EstacoesCLP.MONTAGEM, montagem);
        intervalos.put(EstacoesCLP.EXPEDICAO, expedicao);
    }

    public synchronized long getIntervalo(EstacoesCLP estacao) {
        return intervalos.get(estacao);
    }

    /** Cópia do mapa (ordem do enum) para exposição na API. */
    public synchronized Map<EstacoesCLP, Long> snapshot() {
        return new EnumMap<>(intervalos);
    }

    /** Define o intervalo (ms) da estação. Deve ser {@code > 0} (inválido → 400). */
    public synchronized long setIntervalo(EstacoesCLP estacao, Long intervaloMs) {
        if (intervaloMs == null || intervaloMs <= 0) {
            throw new IllegalArgumentException("Intervalo de polling deve ser > 0 ms: " + intervaloMs);
        }
        Long anterior = intervalos.put(estacao, intervaloMs);
        log.info("Intervalo de polling da estação {} alterado: {} → {} ms",
                estacao.apiName(), anterior, intervaloMs);
        return intervaloMs;
    }
}
