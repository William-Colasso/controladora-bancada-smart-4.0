package com.tecdes.smart.app_smart_40.service.clp.estacao;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.EstoqueCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.EstoqueService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Estação ESTOQUE. Handshake de operação + gestão do magazine (adicionar/remover blocos) com o CLP.
 *
 * <p>Roda sob demanda: {@link #lerEProcessar(String)} lê o bloco DB9 e separa as duas
 * responsabilidades — {@link #lerVariaveis(byte[])} decodifica os bytes no bean (só leitura CLP) e
 * {@link #processarHandshake(PlcConnector)} aplica o handshake (só escrita CLP). O snapshot lido do
 * PLC vive no bean {@link EstoqueCLP} (model/clp), não em campos do service. Sem polling agendado.
 *
 * <p><b>Sem acesso direto ao banco:</b> a única consulta ao banco ({@link EstoqueService#primeiraPosicaoLivre()})
 * passa pelo domínio ({@link EstoqueService}) — este service não conhece repositórios. Também <b>sem
 * caminho CLP→banco</b>: o banco é o mestre do magazine; a escrita banco→CLP fica em {@code EstoqueClpWriter},
 * acionada pelo {@code EstoqueService} quando o banco muda.
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
    private final EstoqueCLP estoqueCLP;

    @Override
    public EstacoesCLP estacao() {
        return EstacoesCLP.ESTOQUE;
    }

    @Override
    public EstacaoCLP dados() {
        return estoqueCLP;
    }

    /** Lê o bloco DB9 da estação ESTOQUE no IP informado, decodifica e processa, sob demanda. */
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
            log.error("Erro ao ler CLP ESTOQUE {}: {}", ip, e.getMessage());
            return false;
        }
    }

    /** Só leitura CLP: decodifica o bloco lido no bean {@link EstoqueCLP}. Não escreve no CLP nem no banco. */
    void lerVariaveis(byte[] dados) {
        estado.setUltimoLeituraMillis(System.currentTimeMillis()); // frescor → gate do estacao-all

        estoqueCLP.setRecebidoOp((dados[0] & 0x01) != 0);

        estoqueCLP.setIniciarPedido((dados[62] & (byte) 0x01) != 0);
        estoqueCLP.setRecebidoEstoque((dados[64] & 0x01) != 0);
        estoqueCLP.setIniciarGuardarEst((dados[64] & 0x02) != 0);

        estoqueCLP.setPosicaoGuardarEst(((dados[66] & 0xFF) << 8) | (dados[67] & 0xFF));

        int[] posicoesOcupadas = new int[28];
        for (int c = 0; c < 28; c++) {
            posicoesOcupadas[c] = dados[68 + c] & 0xFF;
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
    }

    /** Só escrita CLP: handshake sobre o estado já decodificado no bean. Banco apenas via {@link EstoqueService}. */
    void processarHandshake(PlcConnector connector) {
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

        // Handshake físico do magazine no CLP (ack + byte da memória). NÃO escreve no banco:
        // o banco é o mestre e só muda via EstoqueService/API → EstoqueClpWriter (banco→CLP).
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
                } catch (Exception e) {
                    log.error("ERRO: Na tentativa de remover do Estoque", e);
                }
            }
        }

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
                int posEstoqueLivre = estoqueService.primeiraPosicaoLivre();
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
}
