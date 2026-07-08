package com.tecdes.smart.app_smart_40.service.sse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoAllData;
import com.tecdes.smart.app_smart_40.dto.event.EstacaoHeartbeat;
import com.tecdes.smart.app_smart_40.dto.event.EstacaoStatusEvent;
import com.tecdes.smart.app_smart_40.dto.event.EstoqueGridEvent;
import com.tecdes.smart.app_smart_40.dto.event.EstoqueMudou;
import com.tecdes.smart.app_smart_40.dto.response.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.service.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.ProcessoCLP;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.EstoqueService;
import com.tecdes.smart.app_smart_40.service.ExpedicaoService;

import tools.jackson.databind.json.JsonMapper;

/**
 * O coordenador é o único ponto "mudou → publica": passada do write path (estacao-all/status
 * on-change + heartbeat sempre), mutação de banco (grid on-change) e watchdog offline.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClpEventoCoordinator")
class ClpEventoCoordinatorTest {

    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private SseEmitterRegistry sseRegistry;
    @Mock
    private EstadoProducaoService estado;
    @Mock
    private EstoqueService estoqueService;
    @Mock
    private ExpedicaoService expedicaoService;

    private ClpEventoCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new ClpEventoCoordinator(publisher, sseRegistry, estado,
                estoqueService, expedicaoService, JsonMapper.builder().build());
    }

    // ── Passada do write path ────────────────────────────────────────────────

    @Test
    @DisplayName("passada publica heartbeat sempre, estacao-all/status só na mudança")
    void passadaOnChange() {
        ProcessoCLP bean = new ProcessoCLP();
        bean.setOcupado(true);

        coordinator.aoPassadaLida(EstacoesCLP.PROCESSO, bean); // 1ª: tudo novo → publica
        coordinator.aoPassadaLida(EstacoesCLP.PROCESSO, bean); // igual → só heartbeat

        verify(publisher, times(2)).publishEvent(any(EstacaoHeartbeat.class));
        verify(publisher, times(1)).publishEvent(any(EstacaoAllData.class));
        verify(publisher, times(1)).publishEvent(any(EstacaoStatusEvent.class));
    }

    @Test
    @DisplayName("variável do bean muda → estacao-all republica")
    void beanMudou() {
        ProcessoCLP bean = new ProcessoCLP();

        coordinator.aoPassadaLida(EstacoesCLP.PROCESSO, bean);
        bean.setNumeroOP(7);
        coordinator.aoPassadaLida(EstacoesCLP.PROCESSO, bean);

        verify(publisher, times(2)).publishEvent(any(EstacaoAllData.class));
    }

    @Test
    @DisplayName("ocupado → pause/1 (mapping atual)")
    void statusOcupado() {
        ProcessoCLP bean = new ProcessoCLP();
        bean.setOcupado(true);

        coordinator.aoPassadaLida(EstacoesCLP.PROCESSO, bean);

        verify(publisher).publishEvent(new EstacaoStatusEvent("processo", "pause", 1));
    }

    @Test
    @DisplayName("emergencia → off/null")
    void statusEmergencia() {
        ProcessoCLP bean = new ProcessoCLP();
        bean.setEmergencia(true);

        coordinator.aoPassadaLida(EstacoesCLP.PROCESSO, bean);

        verify(publisher).publishEvent(new EstacaoStatusEvent("processo", "off", null));
    }

    @Test
    @DisplayName("aguardando → on/0")
    void statusAguardando() {
        ProcessoCLP bean = new ProcessoCLP();
        bean.setAguardando(true);

        coordinator.aoPassadaLida(EstacoesCLP.PROCESSO, bean);

        verify(publisher).publishEvent(new EstacaoStatusEvent("processo", "on", 0));
    }

    // ── Mutações do banco (grids) ────────────────────────────────────────────

    @Test
    @DisplayName("grid de estoque publica só quando o conteúdo muda")
    void gridOnChange() {
        List<EstoqueResponseDTO> a = List.of(new EstoqueResponseDTO(1L, 1, CorBloco.PRETO));
        when(estoqueService.getTodos()).thenReturn(a, a,
                List.of(new EstoqueResponseDTO(1L, 1, CorBloco.VAZIO)));

        coordinator.onEstoqueMudou(new EstoqueMudou()); // novo → publica
        coordinator.onEstoqueMudou(new EstoqueMudou()); // igual → não
        coordinator.onEstoqueMudou(new EstoqueMudou()); // mudou → publica

        verify(publisher, times(2)).publishEvent(any(EstoqueGridEvent.class));
    }

    // ── Watchdog offline ─────────────────────────────────────────────────────

    @Test
    @DisplayName("sem leitura recente → publica off para as 4 estações (uma vez)")
    void watchdogOffline() {
        when(sseRegistry.count()).thenReturn(1);
        when(estado.getUltimoLeituraMillis()).thenReturn(0L); // stale

        coordinator.watchdogOffline();
        coordinator.watchdogOffline(); // on-change: já está off → não republica

        verify(publisher, times(4)).publishEvent(any(EstacaoStatusEvent.class));
    }

    @Test
    @DisplayName("sem clientes SSE → não publica e limpa caches (reemite ao reconectar)")
    void watchdogSemClientes() {
        ProcessoCLP bean = new ProcessoCLP();
        coordinator.aoPassadaLida(EstacoesCLP.PROCESSO, bean); // popula caches

        when(sseRegistry.count()).thenReturn(0);
        coordinator.watchdogOffline(); // limpa caches, nada publicado além do que já foi

        coordinator.aoPassadaLida(EstacoesCLP.PROCESSO, bean); // caches limpos → republica

        verify(publisher, times(2)).publishEvent(any(EstacaoAllData.class));
        verify(publisher, never()).publishEvent(any(EstoqueGridEvent.class));
    }
}
