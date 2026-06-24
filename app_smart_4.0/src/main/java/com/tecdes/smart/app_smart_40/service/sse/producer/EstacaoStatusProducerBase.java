package com.tecdes.smart.app_smart_40.service.sse.producer;

import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoStatusEvent;
import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

import lombok.extern.slf4j.Slf4j;

/**
 * Produtor read-only do status de uma estação do CLP. Captura + detecta mudança; NÃO conhece o SSE.
 *
 * <p><b>Somente-leitura:</b> usa exclusivamente {@link PlcConnector#readBlock}. Nunca escreve no PLC,
 * nunca chama {@code lerEProcessar}/{@code processData} e não muta os beans {@code *CLP} nem o
 * {@code EstadoProducaoService} (o handshake de escrita permanece nas {@code *ClpService}, a ser
 * disparado por outra via REST).
 *
 * <p>O IP da estação vem do {@link ClpIpRegistry} e é lido <b>a cada ciclo</b> — assim a troca de IP
 * via {@code PUT /api/clp/ips/{estacao}} entra em vigor sem reiniciar. Cada subclasse fixa apenas a
 * estação (DB/tamanho do bloco + offsets dos bytes de status). Só publica quando o snapshot muda.
 *
 * <p><b>Gating por cliente:</b> só lê o CLP quando há ao menos um cliente SSE conectado
 * ({@link SseEmitterRegistry#count()} &gt; 0). Sem ninguém ouvindo, o {@code poll()} retorna cedo e
 * não toca o socket S7 — qualquer tela que abra o {@code EventSource} (home, estações, dashboard) já
 * basta para ligar a leitura; não há mais opt-in manual por estação.
 */
@Slf4j
public abstract class EstacaoStatusProducerBase {

    private final PlcConnectionService plcConnectionService;
    private final ApplicationEventPublisher publisher;
    private final ClpIpRegistry ipRegistry;
    private final SseEmitterRegistry sseRegistry;

    private final EstacaoClp estacao;
    private final int db;
    private final int size;
    /** Índice do byte com os bits cancelOP(0x01)/finishOP(0x02)/startOP(0x04). */
    private final int opByte;
    /** Índice do byte com ocupado(0x01)/aguardando(0x02)/manual(0x04)/emergencia(0x08). */
    private final int flagsByte;

    /** Último evento publicado (cache para detecção de mudança — único estado mantido). */
    private EstacaoStatusEvent ultimo;

    protected EstacaoStatusProducerBase(PlcConnectionService plcConnectionService,
            ApplicationEventPublisher publisher, ClpIpRegistry ipRegistry,
            SseEmitterRegistry sseRegistry, EstacaoClp estacao,
            int db, int size, int opByte, int flagsByte) {
        this.plcConnectionService = plcConnectionService;
        this.publisher = publisher;
        this.ipRegistry = ipRegistry;
        this.sseRegistry = sseRegistry;
        this.estacao = estacao;
        this.db = db;
        this.size = size;
        this.opByte = opByte;
        this.flagsByte = flagsByte;
    }

    @Scheduled(fixedDelayString = "${clp.poll.interval:1000}")
    public void poll() {
        if (sseRegistry.count() == 0) {
            ultimo = null; // ao reconectar, força reemissão do snapshot (não fica preso no cache antigo)
            return;        // ninguém ouvindo o SSE → não lê o socket
        }
        EstacaoStatusEvent atual = capturar();
        if (!Objects.equals(atual, ultimo)) {
            ultimo = atual;
            // Camada anterior ao SSE: o que será publicado, em transição (timeline limpa no INFO).
            log.info("[CLP {}] status -> estado={} funcionamento={} (publicando no SSE)",
                    estacao.apiName(), atual.estado(), atual.funcionamento());
            publisher.publishEvent(atual);
        }
    }

    /** Lê (read-only) os bytes de status e deriva o evento; estação offline → estado "off". */
    private EstacaoStatusEvent capturar() {
        String ip = ipRegistry.getIp(estacao);
        if (ip == null || ip.isBlank()) {
            log.debug("[CLP {}] sem IP configurado -> offline", estacao.apiName());
            return offline();
        }
        PlcConnector connector = plcConnectionService.getConnection(ip);
        if (connector == null) {
            log.debug("[CLP {}] ip={} sem conexão (getConnection null) -> offline", estacao.apiName(), ip);
            return offline();
        }
        try {
            byte[] b;
            synchronized (connector) { // evita leitura concorrente no mesmo socket S7
                b = connector.readBlock(db, 0, size);
            }
            if (b == null || b.length <= flagsByte) {
                log.debug("[CLP {}] ip={} bloco DB{} curto/nulo ({} bytes, esperado >{}) -> offline",
                        estacao.apiName(), ip, db, b == null ? 0 : b.length, flagsByte);
                return offline();
            }
            return derivar(ip, b);
        } catch (Exception e) {
            log.debug("[CLP {}] ip={} falha na leitura do DB{}: {} -> offline",
                    estacao.apiName(), ip, db, e.getMessage());
            plcConnectionService.disconnect(ip); // evicta connector morto → reconecta no próximo ciclo
            return offline();
        }
    }

    private EstacaoStatusEvent derivar(String ip, byte[] b) {
        boolean cancel = (b[opByte] & 0x01) != 0;
        boolean finish = (b[opByte] & 0x02) != 0;
        boolean start = (b[opByte] & 0x04) != 0;

        boolean ocupado = (b[flagsByte] & 0x01) != 0;
        boolean aguardando = (b[flagsByte] & 0x02) != 0;
        boolean manual = (b[flagsByte] & 0x04) != 0;
        boolean emergencia = (b[flagsByte] & 0x08) != 0;

        String estado;
        if (emergencia) {
            estado = "off";
        } else if (aguardando || manual) {
            estado = "pause";
        } else if (ocupado) {
            estado = "on";
        } else {
            estado = "off";
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

        // Camada anterior ao SSE: leitura bruta do CLP (bytes + flags) e o que foi derivado.
        // DEBUG = cada ciclo de leitura (habilite logging.level...producer=DEBUG para ver).
        if (log.isDebugEnabled()) {
            log.debug("[CLP {}] ip={} DB{} op[{}]={} flags[{}]={} | cancel={} finish={} start={} "
                    + "ocupado={} aguardando={} manual={} emergencia={} => estado={} funcionamento={}",
                    estacao.apiName(), ip, db, opByte, hex(b[opByte]), flagsByte, hex(b[flagsByte]),
                    cancel, finish, start, ocupado, aguardando, manual, emergencia, estado, funcionamento);
        }

        return new EstacaoStatusEvent(estacao.getFrontKey(), estado, funcionamento);
    }

    private EstacaoStatusEvent offline() {
        return new EstacaoStatusEvent(estacao.getFrontKey(), "off", null);
    }

    /** Byte em hex (ex.: 0x0A) para inspeção dos bits brutos do bloco do CLP. */
    private static String hex(byte b) {
        return String.format("0x%02X", b);
    }
}
