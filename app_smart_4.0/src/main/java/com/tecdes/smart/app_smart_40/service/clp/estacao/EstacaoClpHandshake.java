package com.tecdes.smart.app_smart_40.service.clp.estacao;

import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;


/**
 * Contrato do handshake de escrita de uma estação do CLP.
 *
 * <p>Implementado pelos quatro {@code *ClpService}. Permite ao {@code ClpComandoService} despachar
 * uma passada de leitura+escrita ({@link #lerEProcessar(String)}) para a estação certa a partir do
 * {@link EstacaoClp}, sem conhecer a implementação concreta. É o lado <b>escrita</b> do CLP — o lado
 * leitura é o módulo SSE read-only (que nunca chama isto).
 */
public interface EstacaoClpHandshake {

    /** Estação física que este serviço controla. */
    EstacoesCLP estacao();

    /**
     * Lê o bloco DB da estação no {@code ip} informado e executa o handshake de escrita, sob demanda.
     *
     * @return {@code true} se a leitura+processamento ocorreu (conexão ok); {@code false} se a estação
     *         está inalcançável (sem conexão / erro de leitura). É o sinal de "passada lida com sucesso"
     *         que o {@code ClpComandoService} usa para pulsar o heartbeat de leitura.
     */
    boolean lerEProcessar(String ip);

    /** Snapshot atual do bean {@code *CLP} desta estação (preenchido pela última passada de {@link #lerEProcessar}). */
    EstacaoCLP dados();
}
