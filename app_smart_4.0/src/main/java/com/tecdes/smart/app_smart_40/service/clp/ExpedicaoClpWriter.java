package com.tecdes.smart.app_smart_40.service.clp;

import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Component
@Slf4j
public class ExpedicaoClpWriter {

    private static final int DB = 9;
    private static final int BASE_MAGAZINE = 6; // byte da posição 1; posição N = BASE + (N-1)

    private final PlcConnectionService plcConnectionService;
    private final ClpIpRegistry ipRegistry;

    /**
     * Espelha a cor (0=vazio,1,2,3) da posição (1..28) na memória do magazine do
     * CLP de ESTOQUE.
     */
    public void escreverPosicao(int posicao, int op) {
        String ip = ipRegistry.getIp(EstacoesCLP.ESTOQUE);
        if (ip == null || ip.isBlank()) {
            log.warn("Banco→CLP: IP do ESTOQUE não configurado — posição {} não enviada ao CLP.", posicao);
            return;
        }
        PlcConnector connector = plcConnectionService.getConnection(ip);
        if (connector == null) {
            log.warn("Banco→CLP: CLP de ESTOQUE inacessível ({}) — posição {} não enviada.", ip, posicao);
            return;
        }
        try {
            synchronized (connector) { // serializa com o read path, que usa o mesmo socket S7
                connector.writeInt(DB, ((posicao - 1) * 2) + 6, op);
            }
        } catch (Exception e) {
            log.error("Banco→CLP: falha ao escrever posição {} (OrdemProducao {}): {}", posicao, op, e.getMessage());
        }
    }
}
