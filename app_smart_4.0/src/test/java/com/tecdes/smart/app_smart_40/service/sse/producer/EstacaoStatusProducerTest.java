package com.tecdes.smart.app_smart_40.service.sse.producer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoStatusEvent;
import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.EstoqueCLP;
import com.tecdes.smart.app_smart_40.model.clp.ProcessoCLP;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;
import com.tecdes.smart.app_smart_40.service.sse.producer.status.EstoqueStatusProducer;
import com.tecdes.smart.app_smart_40.service.sse.producer.status.ProcessoStatusProducer;

/**
 * O status agora deriva do bean {@code *CLP} (preenchido pelo write path), não do socket. Os casos
 * setam os flags no bean + frescor e verificam o {@link EstacaoStatusEvent} publicado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EstacaoStatusProducer (deriva do bean)")
class EstacaoStatusProducerTest {

    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private SseEmitterRegistry sseRegistry;
    @Mock
    private EstadoProducaoService estado;

    private ProcessoStatusProducer producer(ProcessoCLP bean) {
        when(sseRegistry.count()).thenReturn(1);
        return new ProcessoStatusProducer(publisher, sseRegistry, estado, bean);
    }

    private void fresco() {
        when(estado.getUltimoLeituraMillis()).thenReturn(System.currentTimeMillis());
    }

    @Test
    @DisplayName("ocupado → estado on, funcionamento 0")
    void ocupado() {
        ProcessoCLP b = new ProcessoCLP();
        b.setOcupado(true);
        fresco();

        producer(b).poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("processo", "on", 0));
    }

    @Test
    @DisplayName("startOP + ocupado → estado on, funcionamento 1")
    void start() {
        ProcessoCLP b = new ProcessoCLP();
        b.setOcupado(true);
        b.setStartOP(true);
        fresco();

        producer(b).poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("processo", "on", 1));
    }

    @Test
    @DisplayName("finishOP → funcionamento 2")
    void finish() {
        ProcessoCLP b = new ProcessoCLP();
        b.setOcupado(true);
        b.setFinishOP(true);
        fresco();

        producer(b).poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("processo", "on", 2));
    }

    @Test
    @DisplayName("aguardando → estado pause, funcionamento null")
    void aguardando() {
        ProcessoCLP b = new ProcessoCLP();
        b.setAguardando(true);
        fresco();

        producer(b).poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("processo", "pause", null));
    }

    @Test
    @DisplayName("emergencia → estado off (funcionamento independe: sem ocupado → null)")
    void emergencia() {
        ProcessoCLP b = new ProcessoCLP();
        b.setEmergencia(true);
        fresco();

        producer(b).poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("processo", "off", null));
    }

    @Test
    @DisplayName("leitura stale (comunicação parada) → estado off (UX atual)")
    void stale() {
        ProcessoCLP b = new ProcessoCLP();
        b.setOcupado(true);
        when(estado.getUltimoLeituraMillis()).thenReturn(0L); // muito antigo → offline

        producer(b).poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("processo", "off", null));
    }

    @Test
    @DisplayName("publica só quando o status muda")
    void onChange() {
        ProcessoCLP b = new ProcessoCLP();
        b.setOcupado(true);
        when(estado.getUltimoLeituraMillis()).thenReturn(System.currentTimeMillis());

        ProcessoStatusProducer p = producer(b);
        p.poll();
        p.poll();

        verify(publisher, times(1)).publishEvent(any(EstacaoStatusEvent.class));
    }

    @Test
    @DisplayName("Estoque deriva do seu próprio bean (frontKey 'estoque')")
    void estoque() {
        EstoqueCLP b = new EstoqueCLP();
        b.setOcupado(true);
        when(sseRegistry.count()).thenReturn(1);
        when(estado.getUltimoLeituraMillis()).thenReturn(System.currentTimeMillis());

        new EstoqueStatusProducer(publisher, sseRegistry, estado, b).poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("estoque", "on", 0));
    }

    @Test
    @DisplayName("sem clientes SSE → não publica")
    void semClientes() {
        when(sseRegistry.count()).thenReturn(0);

        new ProcessoStatusProducer(publisher, sseRegistry, estado, new ProcessoCLP()).poll();

        verify(publisher, never()).publishEvent(any());
    }
}
