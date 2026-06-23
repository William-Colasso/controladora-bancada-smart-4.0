package com.tecdes.smart.app_smart_40.service.clp;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;

import lombok.extern.slf4j.Slf4j;

/**
 * Registro mutável, em tempo de execução, do IP do CLP de cada estação.
 *
 * <p>Semeado no boot pelos defaults em {@code application.properties} ({@code clp.ip.*}) e alterável
 * via REST ({@code PUT /api/clp/ips/{estacao}}). Os produtores read-only do SSE consultam {@link #getIp}
 * a cada ciclo — assim a troca de IP entra em vigor sem reiniciar a aplicação.
 *
 * <p>Thread-safe: produtores (threads do scheduler) leem; a API escreve. Ao trocar um IP, a conexão
 * antiga é fechada se nenhuma outra estação ainda a usa (o pool reconecta sozinho no IP novo).
 */
@Component
@Slf4j
public class ClpIpRegistry {

    private final PlcConnectionService plcConnectionService;
    private final Map<EstacaoClp, String> ips = new EnumMap<>(EstacaoClp.class);

    public ClpIpRegistry(PlcConnectionService plcConnectionService,
            @Value("${clp.ip.estoque:10.74.241.10}") String estoque,
            @Value("${clp.ip.processo:10.74.241.10}") String processo,
            @Value("${clp.ip.montagem:10.74.241.10}") String montagem,
            @Value("${clp.ip.expedicao:10.74.241.10}") String expedicao) {
        this.plcConnectionService = plcConnectionService;
        ips.put(EstacaoClp.ESTOQUE, estoque);
        ips.put(EstacaoClp.PROCESSO, processo);
        ips.put(EstacaoClp.MONTAGEM, montagem);
        ips.put(EstacaoClp.EXPEDICAO, expedicao);
    }

    public synchronized String getIp(EstacaoClp estacao) {
        return ips.get(estacao);
    }

    /** Cópia do mapa (ordem do enum) para exposição na API. */
    public synchronized Map<EstacaoClp, String> snapshot() {
        return new EnumMap<>(ips);
    }

    /**
     * Define o IP da estação (validado). Retorna o IP normalizado efetivamente gravado.
     * Fecha a conexão do IP anterior se ele não for mais usado por nenhuma estação.
     */
    public synchronized String setIp(EstacaoClp estacao, String ip) {
        String novo = validar(ip);
        String anterior = ips.put(estacao, novo);
        if (anterior != null && !anterior.equals(novo) && !ips.containsValue(anterior)) {
            plcConnectionService.disconnect(anterior);
        }
        log.info("IP da estação {} alterado: {} → {}", estacao.apiName(), anterior, novo);
        return novo;
    }

    private static String validar(String ip) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("IP é obrigatório.");
        }
        String t = ip.trim();
        if (!t.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            throw new IllegalArgumentException("IP inválido: " + ip);
        }
        for (String octeto : t.split("\\.")) {
            if (Integer.parseInt(octeto) > 255) {
                throw new IllegalArgumentException("IP inválido: " + ip);
            }
        }
        return t;
    }
}
