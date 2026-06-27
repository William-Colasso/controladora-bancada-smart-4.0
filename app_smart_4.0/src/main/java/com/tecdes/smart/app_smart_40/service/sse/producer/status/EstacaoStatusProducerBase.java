package com.tecdes.smart.app_smart_40.service.sse.producer.status;

import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoStatusEvent;
import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

import lombok.extern.slf4j.Slf4j;

/**
 * Produtor do status de uma estação derivado do bean {@code *CLP} (preenchido pelo write path).
 *
 * <p><b>Não lê o socket S7.</b> A leitura/processamento ficou centralizada na passada de handshake
 * ({@code *ClpService.lerEProcessar} → {@code processData}); este produtor só <b>deriva</b>
 * estado/funcionamento dos campos já gravados no bean (mesma lógica de antes, sem manipular bytes).
 *
 * <p><b>Gate por cliente SSE:</b> {@code poll()} retorna cedo quando {@link SseEmitterRegistry#count()}
 * é 0 (limpa o cache para reemitir ao reconectar). <b>Gate de frescor:</b> sem leitura recente
 * ({@link EstadoProducaoService#getUltimoLeituraMillis()} há mais que {@link #FRESCOR_MS}), a estação é
 * tratada como offline e emite {@code "off"} (preserva a UX: emergência/comunicação parada → desligado).
 * Publica <b>só on-change</b> vs o último evento.
 */
@Slf4j
public abstract class EstacaoStatusProducerBase {

    /** Janela de frescor: sem leitura há mais que isso, a estação é considerada offline ("off"). */
    private static final long FRESCOR_MS = 1500;

    private final ApplicationEventPublisher publisher;
    private final SseEmitterRegistry sseRegistry;
    private final EstadoProducaoService estado;
    private final EstacaoCLP dados;
    private final EstacoesCLP estacao;

    /** Último evento publicado (cache para detecção de mudança — único estado mantido). */
    private EstacaoStatusEvent ultimo;

    protected EstacaoStatusProducerBase(ApplicationEventPublisher publisher, SseEmitterRegistry sseRegistry,
            EstadoProducaoService estado, EstacaoCLP dados, EstacoesCLP estacao) {
        this.publisher = publisher;
        this.sseRegistry = sseRegistry;
        this.estado = estado;
        this.dados = dados;
        this.estacao = estacao;
    }

    @Scheduled(fixedDelayString = "${clp.poll.interval:1000}")
    public void poll() {
        if (sseRegistry.count() == 0) {
            ultimo = null; // ao reconectar, força reemissão do snapshot (não fica preso no cache antigo)
            return;        // ninguém ouvindo o SSE
        }
        boolean fresco = System.currentTimeMillis() - estado.getUltimoLeituraMillis() <= FRESCOR_MS;
        EstacaoStatusEvent atual = fresco ? derivar() : offline();
        if (!Objects.equals(atual, ultimo)) {
            ultimo = atual;
            // Camada anterior ao SSE: o que será publicado, em transição (timeline limpa no INFO).
            log.info("[CLP {}] status -> estado={} funcionamento={} (publicando no SSE)",
                    estacao.apiName(), atual.estado(), atual.funcionamento());
            publisher.publishEvent(atual);
        }
    }

    /** Deriva estado/funcionamento dos campos já lidos no bean (mesma lógica de bits, sem os bytes). */
    private EstacaoStatusEvent derivar() {
        boolean finish = dados.isFinishOP();
        boolean start = dados.isStartOP();
        boolean ocupado = dados.isOcupado();
        boolean aguardando = dados.isAguardando();
        boolean manual = dados.isManual();
        boolean emergencia = dados.isEmergencia();

        String estadoStr;
        if (emergencia) {
            estadoStr = "off";
        } else if (aguardando || manual) {
            estadoStr = "pause";
        } else if (ocupado) {
            estadoStr = "on";
        } else {
            estadoStr = "off";
        }

        Integer funcionamento;
        if (finish) {
            funcionamento = 2;
        } else if (start) {
            funcionamento = 1;
        } else if (ocupado) {
            funcionamento = 0;
        } else {
            funcionamento = null;
        }

        return new EstacaoStatusEvent(estacao.getFrontKey(), estadoStr, funcionamento);
    }

    private EstacaoStatusEvent offline() {
        return new EstacaoStatusEvent(estacao.getFrontKey(), "off", null);
    }
}
