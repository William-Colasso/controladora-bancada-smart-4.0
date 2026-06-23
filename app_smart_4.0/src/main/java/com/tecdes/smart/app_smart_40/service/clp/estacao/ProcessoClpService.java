package com.tecdes.smart.app_smart_40.service.clp.estacao;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.ProcessoCLP;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Estação PROCESSO. Faz o handshake de operação (RecebidoOP) com o CLP.
 *
 * <p>Roda sob demanda: {@link #lerEProcessar(String)} lê o bloco DB da estação e processa.
 * O snapshot lido do PLC vive no bean {@link ProcessoCLP} (model/clp), não em campos do service.
 * Sem polling agendado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessoClpService {

    private static final int DB = 2;
    private static final int OFFSET = 0;
    private static final int SIZE = 9;

    private final PlcConnectionService plcConnectionService;
    private final EstadoProducaoService estado;
    private final ProcessoCLP processoCLP;

    /** Lê o bloco DB da estação PROCESSO no IP informado e processa, sob demanda. */
    public void lerEProcessar(String ip) {
        PlcConnector connector = plcConnectionService.getConnection(ip);
        if (connector == null) {
            return;
        }
        try {
            byte[] dados = connector.readBlock(DB, OFFSET, SIZE);
            processData(ip, dados);
        } catch (Exception e) {
            log.error("Erro ao ler CLP PROCESSO {}: {}", ip, e.getMessage());
        }
    }

    void processData(String ip, byte[] dados) {
        PlcConnector connector = plcConnectionService.getConnection(ip);
        if (connector == null) {
            return;
        }

        // -------------- Leitura das variáveis → ProcessoCLP -------------------
        processoCLP.setRecebidoOp((dados[0] & 0x01) != 0);

        processoCLP.setNumeroOP(((dados[2] & 0xFF) << 8) | (dados[3] & 0xFF));
        processoCLP.setCancelOP((dados[4] & 0x01) != 0);
        processoCLP.setFinishOP((dados[4] & 0x02) != 0);
        processoCLP.setStartOP((dados[4] & 0x04) != 0);

        processoCLP.setOcupado((dados[6] & 0x01) != 0);
        processoCLP.setAguardando((dados[6] & 0x02) != 0);
        processoCLP.setManual((dados[6] & 0x04) != 0);
        processoCLP.setEmergencia((dados[6] & 0x08) != 0);

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
