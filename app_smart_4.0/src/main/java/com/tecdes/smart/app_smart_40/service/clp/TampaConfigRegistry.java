package com.tecdes.smart.app_smart_40.service.clp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuração em runtime da controladora de tampa (ESP32) — presente só em algumas bancadas.
 *
 * <p>Semeada no boot por {@code tampa.habilitada}/{@code tampa.ip} e alterável via REST
 * ({@code PUT /api/config/tampa}). Quando desabilitada, {@code SmartService.enviarTampa} vira
 * no-op silencioso (a bancada atual pode não ter o seletor físico).
 */
@Component
@Slf4j
public class TampaConfigRegistry {

    private boolean habilitada;
    private String ip;

    public TampaConfigRegistry(
            @Value("${tampa.habilitada:true}") boolean habilitada,
            @Value("${tampa.ip:10.74.241.245}") String ip) {
        this.habilitada = habilitada;
        this.ip = ip;
    }

    public synchronized boolean isHabilitada() {
        return habilitada;
    }

    public synchronized String getIp() {
        return ip;
    }

    /** Atualiza habilitada + IP (validado). Retorna o IP normalizado gravado. */
    public synchronized String atualizar(boolean novaHabilitada, String novoIp) {
        String valido = ClpIpRegistry.validar(novoIp);
        this.habilitada = novaHabilitada;
        this.ip = valido;
        log.info("Controladora de tampa: habilitada={} ip={}", novaHabilitada, valido);
        return valido;
    }
}
