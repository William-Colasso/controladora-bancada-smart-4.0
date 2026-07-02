package com.tecdes.smart.app_smart_40.service.clp;

import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Escrita <b>banco→CLP</b> do magazine de estoque. Quando o banco ({@code EstoqueService}) adiciona ou
 * remove um bloco, espelha a cor na memória do CLP de ESTOQUE (DB9, byte {@code 68 + (posicao-1)} —
 * mesmo offset que o read path lê em {@code posicoesOcupadas}). O banco é o mestre; o CLP é escravo —
 * não há caminho CLP→banco.
 *
 * <p><b>Best-effort:</b> IP não configurado ou CLP inacessível apenas loga (a persistência no banco já
 * ocorreu, igual à política de falhas do {@code SmartService}). O IP é resolvido no {@link ClpIpRegistry}
 * na hora — mesma fonte do read path, então troca de IP vale sem reiniciar.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EstoqueClpWriter {

    private static final int DB = 9;
    private static final int BASE_MAGAZINE = 68; // byte da posição 1; posição N = BASE + (N-1)

    private final PlcConnectionService plcConnectionService;
    private final ClpIpRegistry ipRegistry;

    /** Espelha a cor (0=vazio,1,2,3) da posição (1..28) na memória do magazine do CLP de ESTOQUE. */
    public void escreverPosicao(int posicao, int cor) {
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
                connector.writeByte(DB, BASE_MAGAZINE + (posicao - 1), (byte) cor);
            }
        } catch (Exception e) {
            log.error("Banco→CLP: falha ao escrever posição {} (cor {}): {}", posicao, cor, e.getMessage());
        }
    }
}
