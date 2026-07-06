package com.tecdes.smart.app_smart_40.service.clp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.estacao.EstacaoClpHandshake;
import com.tecdes.smart.app_smart_40.service.sse.ClpEventoCoordinator;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClpComandoService")
class ClpComandoServiceTest {

    @Mock
    private ClpIpRegistry ipRegistry;

    @Mock
    private ClpEventoCoordinator coordinator;

    /** Mock de um handshake fixando sua estação (consumido no construtor do facade). */
    private EstacaoClpHandshake handshake(EstacoesCLP estacao) {
        EstacaoClpHandshake h = mock(EstacaoClpHandshake.class);
        when(h.estacao()).thenReturn(estacao);
        return h;
    }

    @Test
    @DisplayName("processar - resolve o IP no registry e despacha lerEProcessar na estação certa")
    void processar_comIp_despacha() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        EstacaoClpHandshake processo = handshake(EstacoesCLP.PROCESSO);
        ClpComandoService service = new ClpComandoService(List.of(estoque, processo), ipRegistry, coordinator);
        when(ipRegistry.getIp(EstacoesCLP.ESTOQUE)).thenReturn("10.0.0.1");

        service.processar(EstacoesCLP.ESTOQUE);

        verify(estoque).lerEProcessar("10.0.0.1");
        verify(processo, never()).lerEProcessar(anyString());
    }

    @Test
    @DisplayName("processar - leitura ok notifica o coordenador com a estação e o bean lido")
    void processar_comIp_notificaCoordenador() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        when(estoque.lerEProcessar("10.0.0.1")).thenReturn(true);
        ClpComandoService service = new ClpComandoService(List.of(estoque), ipRegistry, coordinator);
        when(ipRegistry.getIp(EstacoesCLP.ESTOQUE)).thenReturn("10.0.0.1");

        service.processar(EstacoesCLP.ESTOQUE);

        verify(estoque).lerEProcessar("10.0.0.1");
        verify(coordinator).aoPassadaLida(EstacoesCLP.ESTOQUE, estoque.dados());
    }

    @Test
    @DisplayName("processar - leitura falha (false) → não notifica o coordenador")
    void processar_leituraFalha_naoNotifica() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        when(estoque.lerEProcessar("10.0.0.1")).thenReturn(false);
        ClpComandoService service = new ClpComandoService(List.of(estoque), ipRegistry, coordinator);
        when(ipRegistry.getIp(EstacoesCLP.ESTOQUE)).thenReturn("10.0.0.1");

        service.processar(EstacoesCLP.ESTOQUE);

        verify(estoque).lerEProcessar("10.0.0.1");
        verify(coordinator, never()).aoPassadaLida(any(), any());
    }

    @Test
    @DisplayName("processar - IP não configurado (blank) → não chama lerEProcessar")
    void processar_semIp_naoDispara() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        ClpComandoService service = new ClpComandoService(List.of(estoque), ipRegistry, coordinator);
        when(ipRegistry.getIp(EstacoesCLP.ESTOQUE)).thenReturn("   ");

        service.processar(EstacoesCLP.ESTOQUE);

        verify(estoque, never()).lerEProcessar(anyString());
        verify(coordinator, never()).aoPassadaLida(any(), any());
    }

    @Test
    @DisplayName("processar - estação sem serviço de handshake → IllegalState (400)")
    void processar_estacaoSemServico_lanca() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        ClpComandoService service = new ClpComandoService(List.of(estoque), ipRegistry, coordinator);

        assertThatThrownBy(() -> service.processar(EstacoesCLP.EXPEDICAO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("processarTodas - despacha em todas as estações com IP")
    void processarTodas_despachaTodas() {
        EstacaoClpHandshake estoque = handshake(EstacoesCLP.ESTOQUE);
        EstacaoClpHandshake processo = handshake(EstacoesCLP.PROCESSO);
        EstacaoClpHandshake montagem = handshake(EstacoesCLP.MONTAGEM);
        EstacaoClpHandshake expedicao = handshake(EstacoesCLP.EXPEDICAO);
        ClpComandoService service = new ClpComandoService(
                List.of(estoque, processo, montagem, expedicao), ipRegistry, coordinator);
        when(ipRegistry.getIp(any())).thenReturn("10.0.0.1");

        service.processarTodas();

        verify(estoque).lerEProcessar("10.0.0.1");
        verify(processo).lerEProcessar("10.0.0.1");
        verify(montagem).lerEProcessar("10.0.0.1");
        verify(expedicao).lerEProcessar("10.0.0.1");
    }
}
