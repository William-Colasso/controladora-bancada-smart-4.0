package com.tecdes.smart.app_smart_40.service.clp.estacao;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.MontagemCLP;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Estação MONTAGEM. Faz o handshake de operação (RecebidoOP) com o CLP.
 *
 * <p>Roda sob demanda: {@link #lerEProcessar(String)} lê o bloco DB da estação e processa.
 * O snapshot lido do PLC vive no bean {@link MontagemCLP} (model/clp), não em campos do service.
 * Sem polling agendado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MontagemClpService {

    private static final int DB = 57;
    private static final int OFFSET = 0;
    private static final int SIZE = 9;

    private final PlcConnectionService plcConnectionService;
    private final EstadoProducaoService estado;
    private final MontagemCLP montagemCLP;

    /** Lê o bloco DB da estação MONTAGEM no IP informado e processa, sob demanda. */
    public void lerEProcessar(String ip) {
        PlcConnector connector = plcConnectionService.getConnection(ip);
        if (connector == null) {
            return;
        }
        try {
            byte[] dados = connector.readBlock(DB, OFFSET, SIZE);
            processData(ip, dados);
        } catch (Exception e) {
            log.error("Erro ao ler CLP MONTAGEM {}: {}", ip, e.getMessage());
        }
    }

    void processData(String ip, byte[] dados) {
        PlcConnector connector = plcConnectionService.getConnection(ip);
        if (connector == null) {
            return;
        }

        // -------------- Leitura das variáveis → MontagemCLP -------------------
        montagemCLP.setRecebidoOp((dados[0] & 0x01) != 0);

        montagemCLP.setNumeroOP(((dados[2] & 0xFF) << 8) | (dados[3] & 0xFF));
        montagemCLP.setCancelOP((dados[4] & 0x01) != 0);
        montagemCLP.setFinishOP((dados[4] & 0x02) != 0);
        montagemCLP.setStartOP((dados[4] & 0x04) != 0);

        montagemCLP.setOcupado((dados[6] & 0x01) != 0);
        montagemCLP.setAguardando((dados[6] & 0x02) != 0);
        montagemCLP.setManual((dados[6] & 0x04) != 0);
        montagemCLP.setEmergencia((dados[6] & 0x08) != 0);

        // Se StartOP, FinishOP e CancelOP estão em FALSE, então RecebidoOP fica em FALSE
        if (!montagemCLP.isStartOP() && !montagemCLP.isFinishOP() && !montagemCLP.isCancelOP()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(57, 0, 0, false); // RecebidoOPMon = FALSE
                } catch (Exception ex) {
                }
            }
        }
        // Início da operação e recebidoOp == FALSE → RecebidoOP fica em TRUE
        if (montagemCLP.isStartOP() && !montagemCLP.isRecebidoOp()) {
            if (estado.getStatusProducao() == 0 && estado.isPedidoEmCurso()) {
                estado.setStatusMontagem(1);
            }
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(57, 0, 0, true); // RecebidoOPMon = TRUE
                } catch (Exception ex) {
                }
            }
        }
        // Término da operação e recebidoOp == FALSE → RecebidoOP fica em TRUE
        if (montagemCLP.isFinishOP() && !montagemCLP.isRecebidoOp()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(57, 0, 0, true); // RecebidoOPMon = TRUE
                } catch (Exception e) {
                    log.error("ERRO [finishOp MONTAGEM]: RecebidoOP [DB57:0.0] para TRUE", e);
                }
                if (estado.getStatusProducao() == 0 && estado.isPedidoEmCurso()) {
                    estado.setStatusMontagem(2);
                }
            }
        }
    }
}
