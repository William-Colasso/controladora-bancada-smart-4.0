package com.tecdes.smart.app_smart_40.service.clp.estacao;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.service.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.ProcessoCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Estação PROCESSO. Faz o handshake de operação (RecebidoOP) com o CLP.
 *
 * <p>Roda sob demanda: {@link #lerEProcessar(String)} lê o bloco DB e separa leitura de escrita —
 * {@link #lerVariaveis(byte[])} decodifica os bytes no bean (só leitura CLP) e
 * {@link #processarHandshake(PlcConnector)} aplica o handshake (só escrita CLP). O snapshot lido do
 * PLC vive no bean {@link ProcessoCLP} (model/clp), não em campos do service. Sem banco. Sem polling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessoClpService implements EstacaoClpHandshake {

    private static final int DB = 2;
    private static final int OFFSET = 0;
    private static final int SIZE = 9;

    private final PlcConnectionService plcConnectionService;
    private final EstadoProducaoService estado;
    private final ProcessoCLP processoCLP;

    @Override
    public EstacoesCLP estacao() {
        return EstacoesCLP.PROCESSO;
    }

    @Override
    public EstacaoCLP dados() {
        return processoCLP;
    }

    /** Lê o bloco DB da estação PROCESSO no IP informado, decodifica e processa, sob demanda. */
    @Override
    public boolean lerEProcessar(String ip) {
        PlcConnector connector = plcConnectionService.getConnection(ip);
        if (connector == null) {
            return false;
        }
        try {
            synchronized (connector) { // serializa com as leituras read-only do SSE no mesmo socket S7
                byte[] dados = connector.readBlock(DB, OFFSET, SIZE);
                lerVariaveis(dados);
                processarHandshake(connector);
            }
            return true;
        } catch (Exception e) {
            log.error("Erro ao ler CLP PROCESSO {}: {}", ip, e.getMessage());
            return false;
        }
    }

    /** Só leitura CLP: decodifica o bloco lido no bean {@link ProcessoCLP}. Não escreve no CLP nem no banco. */
    void lerVariaveis(byte[] dados) {
        estado.setUltimoLeituraMillis(System.currentTimeMillis()); // frescor → gate do estacao-all

        processoCLP.setRecebidoOp((dados[0] & 0x01) != 0);

        processoCLP.setNumeroOP(((dados[2] & 0xFF) << 8) | (dados[3] & 0xFF));
        processoCLP.setCancelOP((dados[4] & 0x01) != 0);
        processoCLP.setFinishOP((dados[4] & 0x02) != 0);
        processoCLP.setStartOP((dados[4] & 0x04) != 0);

        processoCLP.setOcupado((dados[6] & 0x01) != 0);
        processoCLP.setAguardando((dados[6] & 0x02) != 0);
        processoCLP.setManual((dados[6] & 0x04) != 0);
        processoCLP.setEmergencia((dados[6] & 0x08) != 0);
    }

    /** Só escrita CLP: handshake de RecebidoOP sobre o estado já decodificado no bean. */
    void processarHandshake(PlcConnector connector) {
        // Se StartOP, FinishOP e CancelOP estão em FALSE, então RecebidoOP fica em FALSE
        if (!processoCLP.isStartOP() && !processoCLP.isFinishOP() && !processoCLP.isCancelOP()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(2, 0, 0, false); // RecebidoOPPro = FALSE
                } catch (Exception ex) {
                }
            }
        }
        // Início da operação e recebidoOp == FALSE → RecebidoOP fica em TRUE
        if (processoCLP.isStartOP() && !processoCLP.isRecebidoOp()) {
            if (estado.getStatusProducao() == 0 && estado.isPedidoEmCurso()) {
                estado.setStatusProcesso(1);
            }
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(2, 0, 0, true); // RecebidoOPPro = TRUE
                } catch (Exception e) {
                    log.error("ERRO [startOp PROCESSO]: RecebidoOP [DB2:0.0] para TRUE", e);
                }
            }
        }
        // Término da operação e recebidoOp == FALSE → RecebidoOP fica em TRUE
        if (processoCLP.isFinishOP() && !processoCLP.isRecebidoOp()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(2, 0, 0, true); // RecebidoOPPro = TRUE
                } catch (Exception e) {
                    log.error("ERRO [finishOp PROCESSO]: RecebidoOP [DB2:0.0] para TRUE", e);
                }
                if (estado.getStatusProducao() == 0 && estado.isPedidoEmCurso()) {
                    estado.setStatusProcesso(2);
                }
            }
        }
    }
}
