package com.tecdes.smart.app_smart_40.service.clp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.estacao.EstacaoClpHandshake;
import com.tecdes.smart.app_smart_40.service.sse.ClpEventoCoordinator;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClpComandoService")
class ClpComandoServiceTest {

    @Mock
    private ClpIpRegistry ipRegistry;

    @Mock
    private ClpEventoCoordinator coordinator;

    @Mock
    private PlcConnectionService plcConnectionService;

    /** Mock de um handshake fixando sua estação (consumido no construtor do facade). */
    private EstacaoClpHandshake handshake(EstacoesCLP estacao) {
        EstacaoClpHandshake h = mock(EstacaoClpHandshake.class);
        when(h.estacao()).thenReturn(estacao);
        return h;
    }

    private ClpComandoService service(EstacaoClpHandshake... handshakes) {
        return new ClpComandoService(List.of(handshakes), ipRegistry, coordinator, plcConnectionService);
    }

    @Test
    @DisplayName("processar - ping ok resolve o IP e despacha lerEProcessar na estação certa")
    void processar_comIp_despacha() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        EstacaoClpHandshake processo = handshake(EstacoesCLP.PROCESSO);
        ClpComandoService service = service(estoque, processo);
        when(ipRegistry.getIp(EstacoesCLP.ESTOQUE)).thenReturn("10.0.0.1");
        when(plcConnectionService.online("10.0.0.1")).thenReturn(true);

        service.processar(EstacoesCLP.ESTOQUE);

        verify(estoque).lerEProcessar("10.0.0.1");
        verify(processo, never()).lerEProcessar(anyString());
    }

    @Test
    @DisplayName("processar - leitura ok notifica o coordenador com a estação e o bean lido")
    void processar_comIp_notificaCoordenador() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        when(estoque.lerEProcessar("10.0.0.1")).thenReturn(true);
        ClpComandoService service = service(estoque);
        when(ipRegistry.getIp(EstacoesCLP.ESTOQUE)).thenReturn("10.0.0.1");
        when(plcConnectionService.online("10.0.0.1")).thenReturn(true);

        service.processar(EstacoesCLP.ESTOQUE);

        verify(coordinator).aoPing(EstacoesCLP.ESTOQUE, true);
        verify(estoque).lerEProcessar("10.0.0.1");
        verify(coordinator).aoPassadaLida(EstacoesCLP.ESTOQUE, estoque.dados());
    }

    @Test
    @DisplayName("processar - leitura falha (false) → não notifica passada")
    void processar_leituraFalha_naoNotifica() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        when(estoque.lerEProcessar("10.0.0.1")).thenReturn(false);
        ClpComandoService service = service(estoque);
        when(ipRegistry.getIp(EstacoesCLP.ESTOQUE)).thenReturn("10.0.0.1");
        when(plcConnectionService.online("10.0.0.1")).thenReturn(true);

        service.processar(EstacoesCLP.ESTOQUE);

        verify(estoque).lerEProcessar("10.0.0.1");
        verify(coordinator, never()).aoPassadaLida(any(), any());
    }

    @Test
    @DisplayName("processar - ping offline (false) → não lê, não notifica passada, mas registra o ping")
    void processar_pingOffline_naoLe() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        ClpComandoService service = service(estoque);
        when(ipRegistry.getIp(EstacoesCLP.ESTOQUE)).thenReturn("10.0.0.1");
        when(plcConnectionService.online("10.0.0.1")).thenReturn(false);

        service.processar(EstacoesCLP.ESTOQUE);

        verify(coordinator).aoPing(EstacoesCLP.ESTOQUE, false);
        verify(estoque, never()).lerEProcessar(anyString());
        verify(coordinator, never()).aoPassadaLida(any(), any());
    }

    @Test
    @DisplayName("processar - IP não configurado (blank) → nem pinga nem lê")
    void processar_semIp_naoDispara() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        ClpComandoService service = service(estoque);
        when(ipRegistry.getIp(EstacoesCLP.ESTOQUE)).thenReturn("   ");

        service.processar(EstacoesCLP.ESTOQUE);

        verify(plcConnectionService, never()).online(anyString());
        verify(estoque, never()).lerEProcessar(anyString());
        verify(coordinator, never()).aoPassadaLida(any(), any());
    }

    @Test
    @DisplayName("processar - estação sem serviço de handshake → IllegalState (400)")
    void processar_estacaoSemServico_lanca() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        ClpComandoService service = service(estoque);

        assertThatThrownBy(() -> service.processar(EstacoesCLP.EXPEDICAO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("processarTodas - despacha em todas as estações com IP e ping ok")
    void processarTodas_despachaTodas() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        EstacaoClpHandshake processo = handshake(EstacoesCLP.PROCESSO);
        EstacaoClpHandshake montagem = handshake(EstacoesCLP.MONTAGEM);
        EstacaoClpHandshake expedicao = handshake(EstacoesCLP.EXPEDICAO);
        ClpComandoService service = service(estoque, processo, montagem, expedicao);
        when(ipRegistry.getIp(any())).thenReturn("10.0.0.1");
        when(plcConnectionService.online("10.0.0.1")).thenReturn(true);

        service.processarTodas();

        verify(estoque).lerEProcessar("10.0.0.1");
        verify(processo).lerEProcessar("10.0.0.1");
        verify(montagem).lerEProcessar("10.0.0.1");
        verify(expedicao).lerEProcessar("10.0.0.1");
    }

    @Test
    @DisplayName("processarAsync - delega a processar e completa o future")
    void processarAsync_delega() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        ClpComandoService service = service(estoque);
        when(ipRegistry.getIp(EstacoesCLP.ESTOQUE)).thenReturn("10.0.0.1");
        when(plcConnectionService.online("10.0.0.1")).thenReturn(true);

        CompletableFuture<Void> f = service.processarAsync(EstacoesCLP.ESTOQUE);

        assertThat(f).isCompleted();
        verify(estoque).lerEProcessar("10.0.0.1");
    }

    @Test
    @DisplayName("processarAsync - completa o future mesmo se processar lança (não trava o scheduler)")
    void processarAsync_engoleErro() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        ClpComandoService service = service(estoque); // EXPEDICAO não tem handshake → processar lança

        CompletableFuture<Void> f = service.processarAsync(EstacoesCLP.EXPEDICAO);

        assertThat(f).isCompleted();
    }
}
