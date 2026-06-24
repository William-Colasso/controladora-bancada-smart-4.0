package com.tecdes.smart.app_smart_40.service.clp.estacao;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.ExpedicaoCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.repository.ExpedicaoRepository;
import com.tecdes.smart.app_smart_40.service.ExpedicaoService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Estação EXPEDIÇÃO. Handshake de operação + gestão do magazine de expedição (guardar/remover
 * pedidos concluídos) com o CLP.
 *
 * <p>Roda sob demanda: {@link #lerEProcessar(String)} lê o bloco DB9 da estação e processa.
 * O snapshot lido do PLC vive no bean {@link ExpedicaoCLP} (model/clp), não em campos do service.
 * Persistência via {@link ExpedicaoService} (sem HTTP). Sem polling agendado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpedicaoClpService implements EstacaoClpHandshake {

    private static final int DB = 9;
    private static final int OFFSET = 0;
    private static final int SIZE = 48;

    private final PlcConnectionService plcConnectionService;
    private final EstadoProducaoService estado;
    private final ExpedicaoService expedicaoService;
    private final ExpedicaoRepository expedicaoRepository;
    private final ExpedicaoCLP expedicaoCLP;

    @Override
    public EstacaoClp estacao() {
        return EstacaoClp.EXPEDICAO;
    }

    @Override
    public EstacaoCLP dados() {
        return expedicaoCLP;
    }

    /** Lê o bloco DB9 da estação EXPEDIÇÃO no IP informado e processa, sob demanda. */
    @Override
    public void lerEProcessar(String ip) {
        PlcConnector connector = plcConnectionService.getConnection(ip);
        if (connector == null) {
            return;
        }
        try {
            synchronized (connector) { // serializa com as leituras read-only do SSE no mesmo socket S7
                byte[] dados = connector.readBlock(DB, OFFSET, SIZE);
                processData(ip, dados);
            }
        } catch (Exception e) {
            log.error("Erro ao ler CLP EXPEDICAO {}: {}", ip, e.getMessage());
        }
    }

    void processData(String ip, byte[] dados) {
        PlcConnector connector = plcConnectionService.getConnection(ip);
        if (connector == null) {
            return;
        }

        // -------------- Leitura das variáveis → ExpedicaoCLP -------------------
        expedicaoCLP.setRecebidoOp((dados[0] & 0x01) != 0);

        expedicaoCLP.setRecebidoExpedicao((dados[2] & 0x01) != 0);
        expedicaoCLP.setIniciarGuardarExp((dados[2] & 0x02) != 0);
        expedicaoCLP.setPosicaoGuardarExp(((dados[4] & 0xFF) << 8) | (dados[5] & 0xFF));

        int[] orderExpedicao = new int[12];
        int x = 0;
        for (int c = 0; c < 24; c += 2) {
            orderExpedicao[x] = ((dados[c + 6] & 0xFF) << 8) | (dados[c + 7] & 0xFF);
            x++;
        }
        expedicaoCLP.setOrderExpedicao(orderExpedicao);

        expedicaoCLP.setNumeroOP(((dados[30] & 0xFF) << 8) | (dados[31] & 0xFF));
        expedicaoCLP.setCancelOP((dados[32] & 0x01) != 0);
        expedicaoCLP.setFinishOP((dados[32] & 0x02) != 0);
        expedicaoCLP.setStartOP((dados[32] & 0x04) != 0);

        expedicaoCLP.setOcupado((dados[34] & 0x01) != 0);
        expedicaoCLP.setAguardando((dados[34] & 0x02) != 0);
        expedicaoCLP.setManual((dados[34] & 0x04) != 0);
        expedicaoCLP.setEmergencia((dados[34] & 0x08) != 0);

        expedicaoCLP.setPedirPosicaoExp((dados[36] & 0x01) != 0);
        expedicaoCLP.setPosicaoGuardadoExpedicao(((dados[38] & 0xFF) << 8) | (dados[39] & 0xFF));
        expedicaoCLP.setPosicaoRemovidoExpedicao(((dados[40] & 0xFF) << 8) | (dados[41] & 0xFF));
        expedicaoCLP.setAdicionarExpedicao((dados[42] & 0x01) != 0);
        expedicaoCLP.setRemoverExpedicao((dados[42] & 0x02) != 0);
        expedicaoCLP.setOpGuardadoExpedicao(((dados[44] & 0xFF) << 8) | (dados[45] & 0xFF));

        int posicaoGuardarExp = expedicaoCLP.getPosicaoGuardarExp();
        int posicaoRemovidoExpedicao = expedicaoCLP.getPosicaoRemovidoExpedicao();
        int opGuardadoExpedicao = expedicaoCLP.getOpGuardadoExpedicao();

        // StartOP, FinishOP e CancelOP em FALSE → RecebidoOP fica em FALSE
        if (!expedicaoCLP.isStartOP() && !expedicaoCLP.isFinishOP() && !expedicaoCLP.isCancelOP()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 0, 0, false); // RecebidoOPExp = FALSE
                } catch (Exception e) {
                    log.error("ERRO [startOp][finishOp]: RecebidoOPExp [DB9:0.0] para FALSE");
                }
            }
        }

        // Início da operação e recebidoOp == FALSE → RecebidoOP fica em TRUE
        if (expedicaoCLP.isStartOP() && !expedicaoCLP.isRecebidoOp()) {
            if (estado.getStatusProducao() == 0 && estado.isPedidoEmCurso()) {
                estado.setStatusExpedicao(1);
            }
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 0, 0, true); // RecebidoOPExp = TRUE
                } catch (Exception e) {
                    log.error("ERRO [startOp]: RecebidoOPExp [DB9:0.0] para TRUE");
                }
            }
        }

        // Término da operação e recebidoOp == FALSE → RecebidoOP fica em TRUE
        if (expedicaoCLP.isFinishOP() && !expedicaoCLP.isRecebidoOp()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 0, 0, true); // RecebidoOPExp = TRUE
                    estado.setBlockFinished(true);
                } catch (Exception e) {
                    log.error("ERRO [finishOp]: RecebidoOPExp [DB9:0.0] para TRUE");
                }
                if (estado.getStatusProducao() == 0 && estado.isPedidoEmCurso()) {
                    estado.setStatusExpedicao(2);
                }
            }
        }

        if (!expedicaoCLP.isPedirPosicaoExp()) {
            if (!estado.isReadOnly()) {
                estado.setAuxExpedicao(false);
                try {
                    connector.writeBit(9, 2, 1, false); // IniciarGuardar = FALSE
                } catch (Exception e) {
                    log.error("ERRO [Pedir Posição]: IniciarGuardar [DB9:2.1] para FALSE");
                }
            }
        }

        // Expedição pede posição para guardar
        if (expedicaoCLP.isPedirPosicaoExp() && !estado.isAuxExpedicao()) {
            estado.setAuxExpedicao(true);
            if (!estado.isReadOnly()) {
                try {
                    connector.writeInt(9, 4, estado.getPosicaoExpedicaoSolicitada());
                } catch (Exception e) {
                    log.error("ERRO: PosicaoGuardarExpedicao [DB9:4]");
                }
                try {
                    connector.writeBit(9, 2, 1, true); // IniciarGuardar = TRUE
                } catch (Exception e) {
                    log.error("ERRO [Pedir Posição]: IniciarGuardar [DB9:2.1] para TRUE");
                }
            }
        }

        if (!estado.isReadOnly() && (!expedicaoCLP.isAdicionarExpedicao() || !expedicaoCLP.isRemoverExpedicao())) {
            try {
                connector.writeBit(9, 2, 0, false); // RecebidoExpedicao = FALSE
            } catch (Exception e) {
                log.error("ERRO [Adicionar/Remover Expedição]: RecebidoExpedicao [DB9:2.0] para FALSE");
            }
        }

        // adicionarExpedicao TRUE E auxExpedicao FALSE → RecebidoExpedicao TRUE + persiste
        if (expedicaoCLP.isAdicionarExpedicao() && !estado.isAuxExpedicao()) {
            estado.setAuxExpedicao(true);
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 2, 0, true); // RecebidoExpedicao = TRUE
                } catch (Exception e) {
                    log.error("ERRO [Adicionar Expedição]: RecebidoExpedicao [DB9:2.0] para TRUE");
                }

                int offset = 6 + (posicaoGuardarExp - 1) * 2;
                if (posicaoGuardarExp > 0) {
                    try {
                        connector.writeInt(9, offset, opGuardadoExpedicao);
                        expedicaoService.guardarNaPosicao(posicaoGuardarExp, opGuardadoExpedicao);
                        log.info("Guardado OP {} na posição de expedição {}", opGuardadoExpedicao, posicaoGuardarExp);
                    } catch (Exception e) {
                        log.error("ERRO: Na tentativa de adicionar na Expedição", e);
                    }
                }
            }
        }

        // removerExpedicao TRUE E auxExpedicao FALSE → RecebidoExpedicao TRUE + persiste
        if (expedicaoCLP.isRemoverExpedicao() && !estado.isAuxExpedicao()) {
            estado.setAuxExpedicao(true);
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 2, 0, true); // RecebidoExpedicao = TRUE
                } catch (Exception e) {
                    log.error("ERRO [Remover Expedição]: RecebidoExpedicao [DB9:2.0] para TRUE");
                }

                int offset = 6 + (posicaoRemovidoExpedicao - 1) * 2;
                if (posicaoRemovidoExpedicao > 0) {
                    try {
                        connector.writeInt(9, offset, 0);
                        expedicaoService.removerDaPosicao(posicaoRemovidoExpedicao);
                        log.info("Removido OP da posição de expedição {}", posicaoRemovidoExpedicao);
                    } catch (Exception e) {
                        log.error("ERRO: Na tentativa de remover da Expedição", e);
                    }
                }
            }
        }

        // Bloco guardado na posição esperada e estação livre → produção concluída
        if (expedicaoCLP.getPosicaoGuardadoExpedicao() == posicaoGuardarExp
                && !expedicaoCLP.isOcupado() && expedicaoCLP.isFinishOP()) {
            if (!estado.isReadOnly()) {
                if (estado.getStatusProducao() == 0 && estado.isPedidoEmCurso()) {
                    estado.setStatusProducao(1);
                }
                log.info("Operação OP:{} Finalizada", opGuardadoExpedicao);
            }
        }
    }
}
