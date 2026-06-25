package com.tecdes.smart.app_smart_40.service.clp.estacao;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.dto.request.EstoqueRequestDTO;
import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.EstoqueCLP;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;
import com.tecdes.smart.app_smart_40.service.EstoqueService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Estação ESTOQUE. Handshake de operação + gestão do magazine (adicionar/remover blocos) com o CLP.
 *
 * <p>Roda sob demanda: {@link #lerEProcessar(String)} lê o bloco DB9 da estação e processa.
 * O snapshot lido do PLC vive no bean {@link EstoqueCLP} (model/clp), não em campos do service.
 * Persistência via {@link EstoqueService} (sem HTTP). Sem polling agendado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstoqueClpService implements EstacaoClpHandshake {

    private static final int DB = 9;
    private static final int OFFSET = 0;
    private static final int SIZE = 111;

    private final PlcConnectionService plcConnectionService;
    private final EstadoProducaoService estado;
    private final EstoqueService estoqueService;
    private final EstoqueRepository estoqueRepository;
    private final EstoqueCLP estoqueCLP;

    @Override
    public EstacoesCLP estacao() {
        return EstacoesCLP.ESTOQUE;
    }

    @Override
    public EstacaoCLP dados() {
        return estoqueCLP;
    }

    /** Lê o bloco DB9 da estação ESTOQUE no IP informado e processa, sob demanda. */
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
            log.error("Erro ao ler CLP ESTOQUE {}: {}", ip, e.getMessage());
        }
    }

    void processData(String ip, byte[] dados) {
        PlcConnector connector = plcConnectionService.getConnection(ip);
        if (connector == null) {
            return;
        }

        // -------------- Leitura das variáveis → EstoqueCLP -------------------
        estoqueCLP.setRecebidoOp((dados[0] & 0x01) != 0);

        estoqueCLP.setIniciarPedido((dados[62] & (byte) 0x01) != 0);
        estoqueCLP.setRecebidoEstoque((dados[64] & 0x01) != 0);
        estoqueCLP.setIniciarGuardarEst((dados[64] & 0x02) != 0);

        estoqueCLP.setPosicaoGuardarEst(((dados[66] & 0xFF) << 8) | (dados[67] & 0xFF));

        byte[] posicoesOcupadas = new byte[28];
        for (int c = 0; c < 28; c++) {
            posicoesOcupadas[c] = dados[68 + c];
        }
        estoqueCLP.setPosicoesOcupadas(posicoesOcupadas);

        estoqueCLP.setNumeroOP(((dados[96] & 0xFF) << 8) | (dados[97] & 0xFF));
        estoqueCLP.setCancelOP((dados[98] & 0x01) != 0);
        estoqueCLP.setFinishOP((dados[98] & 0x02) != 0);
        estoqueCLP.setStartOP((dados[98] & 0x04) != 0);

        estoqueCLP.setOcupado((dados[100] & 0x01) != 0);
        estoqueCLP.setAguardando((dados[100] & 0x02) != 0);
        estoqueCLP.setManual((dados[100] & 0x04) != 0);
        estoqueCLP.setEmergencia((dados[100] & 0x08) != 0);

        estoqueCLP.setPedirPosicaoEst((dados[102] & 0x01) != 0);
        estoqueCLP.setPosicaoEstoque(((dados[104] & 0xFF) << 8) | (dados[105] & 0xFF));
        estoqueCLP.setAdicionarEstoque((dados[106] & 0x01) != 0);
        estoqueCLP.setRemoverEstoque((dados[106] & 0x02) != 0);
        estoqueCLP.setRetornoEstoqueCheio((dados[106] & 0x04) != 0);
        estoqueCLP.setCorGuardarEstoque(((dados[108] & 0xFF) << 8) | (dados[109] & 0xFF));

        int posicaoEstoque = estoqueCLP.getPosicaoEstoque();
        int corGuardarEstoque = estoqueCLP.getCorGuardarEstoque();

        // Pedido iniciado e estação ESTOQUE OCUPADA → iniciarPedido fica em FALSE
        if (estoqueCLP.isIniciarPedido() && estoqueCLP.isOcupado()) {
            estado.setPedidoEmCurso(true);
            estado.setStatusEstoque(0);
            estado.setStatusProducao(0);
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 62, 0, false); // IniciarPedido = FALSE
                } catch (Exception e) {
                    log.error("ERRO [iniciarPedido & ocupadoEst]: IniciarPedido [DB9:62.0] para FALSE");
                }
            }
        }

        // StartOP, FinishOP e CancelOP em FALSE → RecebidoOP fica em FALSE
        if (!estoqueCLP.isStartOP() && !estoqueCLP.isFinishOP() && !estoqueCLP.isCancelOP()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 0, 0, false); // RecebidoOPEst = FALSE
                } catch (Exception e) {
                    log.error("ERRO: RecebidoOPEstoque [DB9:0.0] para FALSE");
                }
            }
        }

        // Início da operação e recebidoOp == FALSE → RecebidoOP fica em TRUE
        if (estoqueCLP.isStartOP() && !estoqueCLP.isRecebidoOp()) {
            if (estado.getStatusProducao() == 0 && estado.isPedidoEmCurso()) {
                estado.setStatusEstoque(1);
            }
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 0, 0, true); // RecebidoOPEst = TRUE
                } catch (Exception e) {
                    log.error("ERRO [startOp]: RecebidoOPEstoque [DB9:0.0] para TRUE");
                }
            }
        }

        // Término da operação e recebidoOp == FALSE → RecebidoOP fica em TRUE
        if (estoqueCLP.isFinishOP() && !estoqueCLP.isRecebidoOp()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 0, 0, true); // RecebidoOPEst = TRUE
                } catch (Exception e) {
                    log.error("ERRO [finishOp]: RecebidoOPEstoque [DB9:0.0] para TRUE");
                }
                if (estado.getStatusProducao() == 0 && estado.isPedidoEmCurso()) {
                    estado.setStatusEstoque(2);
                }
            }
        }

        // remover e adicionar em FALSE → RecebidoEstoque fica em FALSE
        if (!estoqueCLP.isRemoverEstoque() && !estoqueCLP.isAdicionarEstoque()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 64, 0, false); // RecebidoEstoque = FALSE
                } catch (Exception e) {
                    log.error("ERRO: RecebidoEstoque [DB9:64.0] para FALSE");
                }
            }
        }

        // Remove a posição na tabela Estoque e na memória do CLP
        if (posicaoEstoque > 0 && estoqueCLP.isRemoverEstoque()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 64, 0, true);
                } catch (Exception e) {
                    log.error("ERRO: RecebidoEstoque [DB9:64.0] para TRUE");
                }

                byte offset = (byte) (68 + (posicaoEstoque - 1));
                try {
                    connector.writeByte(9, offset, (byte) 0);
                    estoqueService.removerBloco((byte) posicaoEstoque);
                    log.info("Removido estoque na posição {}", posicaoEstoque);
                } catch (Exception e) {
                    log.error("ERRO: Na tentativa de remover do Estoque", e);
                }
            }
        }

        // Adiciona a cor do bloco na posição (tabela Estoque + memória do CLP)
        if (posicaoEstoque > 0 && estoqueCLP.isAdicionarEstoque()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 64, 0, true); // RecebidoEstoque = TRUE
                } catch (Exception e) {
                    log.error("ERRO: RecebidoEstoque [DB9:64.0] para TRUE");
                }

                byte offset = (byte) (68 + (posicaoEstoque - 1));
                try {
                    connector.writeByte(9, offset, (byte) corGuardarEstoque);
                    estoqueService.adicionarBloco(
                            new EstoqueRequestDTO(posicaoEstoque, CorBloco.fromValue(corGuardarEstoque)));
                    log.info("Adicionado estoque na posição {} cor {}", posicaoEstoque, corGuardarEstoque);
                } catch (Exception e) {
                    log.error("ERRO: Na tentativa de adicionar no Estoque", e);
                }
            }
        }

        // ocupado ou retornoEstoqueCheio em TRUE E iniciarGuardarEst ativo → iniciarGuardarEst em FALSE
        if ((estoqueCLP.isOcupado() || estoqueCLP.isRetornoEstoqueCheio()) && estoqueCLP.isIniciarGuardarEst()) {
            if (!estado.isReadOnly()) {
                try {
                    connector.writeBit(9, 64, 1, false); // iniciarGuardarEst = FALSE
                } catch (Exception e) {
                    log.error("ERRO: IniciarGuardarEstoque [DB9:64.1] para FALSE");
                }
            }
        }

        // Estação livre E pede posição para guardar → localiza posição livre no magazine
        if (estoqueCLP.isPedirPosicaoEst() && !estoqueCLP.isOcupado()) {
            if (!estado.isReadOnly()) {
                int posEstoqueLivre = primeiraPosicaoLivre();
                if (posEstoqueLivre > 0) {
                    try {
                        connector.writeInt(9, 66, posEstoqueLivre); // PosicaoGuardar
                    } catch (Exception e) {
                        log.error("ERRO: PosicaoGuardarEstoque [DB9:66]");
                    }
                    try {
                        connector.writeBit(9, 64, 1, true); // IniciarGuardar = TRUE
                    } catch (Exception e) {
                        log.error("ERRO: IniciarGuardarEstoque [DB9:64.1]");
                    }
                } else {
                    log.warn("ERRO: Nao existe posição livre.");
                }
            }
        }
    }

    /** Primeira posição VAZIA do magazine de estoque, ou -1 se não houver. */
    public int primeiraPosicaoLivre() {
        return estoqueRepository.findPosicoesVazias()
                .stream()
                .map(e -> e.getPosicao())
                .findFirst()
                .orElse(-1);
    }
}
