package com.tecdes.smart.app_smart_40.model.clp;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Estado compartilhado da produção entre as estações do CLP.
 *
 * <p>Substitui os campos {@code static} do antigo {@code SmartService} (que acoplavam as estações
 * por estado global). Aqui é um bean de instância, injetado pelas {@code *ClpService}, mantendo a
 * coordenação do fluxo de produção sem estado estático global.
 */
@Component
@Getter
@Setter
public class EstadoProducaoService {

    /** 0 = ocioso, 1 = iniciou operação, 2 = concluiu operação. */
    private int statusEstoque;
    private int statusProcesso;
    private int statusMontagem;
    private int statusExpedicao;

    /** 0 = produção em andamento, 1 = produção concluída. */
    private int statusProducao;

    private boolean pedidoEmCurso;

    /** Quando true, as estações não escrevem flags no PLC (modo somente-leitura). */
    private boolean readOnly;

    private boolean blockFinished;

    /** Trava de reentrância do handshake da expedição. */
    private boolean auxExpedicao;

    /**
     * Instante (epoch ms) da última leitura bem-sucedida do CLP em qualquer estação. Sinaliza
     * "frescor de leitura": os produtores de {@code estacao-all} só publicam enquanto está recente,
     * e o front mostra "Aguardando comunicação CLP" quando para. Atualizado em cada {@code processData}.
     */
    private volatile long ultimoLeituraMillis;

    /** Posição de expedição solicitada ao PLC para guardar o bloco concluído. */
    private int posicaoExpedicaoSolicitada;
}
