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
import org.springframework.context.ApplicationEventPublisher;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoAllData;
import com.tecdes.smart.app_smart_40.model.clp.EstoqueCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.estacao.EstacaoClpHandshake;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClpComandoService")
class ClpComandoServiceTest {

    @Mock
    private ClpIpRegistry ipRegistry;

    @Mock
    private ApplicationEventPublisher publisher;

    /** Mock de um handshake fixando sua estação (consumido no construtor do facade). */
    private EstacaoClpHandshake handshake(EstacaoClp estacao) {
        EstacaoClpHandshake h = mock(EstacaoClpHandshake.class);
        when(h.estacao()).thenReturn(estacao);
        return h;
    }

    @Test
    @DisplayName("processar - resolve o IP no registry e despacha lerEProcessar na estação certa")
    void processar_comIp_despacha() {
        EstacaoClpHandshake estoque = handshake(EstacaoClp.ESTOQUE);
        EstacaoClpHandshake processo = handshake(EstacaoClp.PROCESSO);
        ClpComandoService service = new ClpComandoService(List.of(estoque, processo), ipRegistry, publisher);
        when(ipRegistry.getIp(EstacaoClp.ESTOQUE)).thenReturn("10.0.0.1");

        service.processar(EstacaoClp.ESTOQUE);

        verify(estoque).lerEProcessar("10.0.0.1");
        verify(processo, never()).lerEProcessar(anyString());
    }

    @Test
    @DisplayName("processar - com IP publica EstacaoAllData(frontKey, dados) no barramento")
    void processar_comIp_publicaEstacaoAll() {
        EstacaoClpHandshake estoque = handshake(EstacaoClp.ESTOQUE);
        EstoqueCLP bean = new EstoqueCLP();
        when(estoque.dados()).thenReturn(bean);
        ClpComandoService service = new ClpComandoService(List.of(estoque), ipRegistry, publisher);
        when(ipRegistry.getIp(EstacaoClp.ESTOQUE)).thenReturn("10.0.0.1");

        service.processar(EstacaoClp.ESTOQUE);

        verify(estoque).lerEProcessar("10.0.0.1");
        verify(publisher).publishEvent(new EstacaoAllData("estoque", bean));
    }

    @Test
    @DisplayName("processar - IP não configurado (blank) → não chama lerEProcessar")
    void processar_semIp_naoDispara() {
        EstacaoClpHandshake estoque = handshake(EstacaoClp.ESTOQUE);
        ClpComandoService service = new ClpComandoService(List.of(estoque), ipRegistry, publisher);
        when(ipRegistry.getIp(EstacaoClp.ESTOQUE)).thenReturn("   ");

        service.processar(EstacaoClp.ESTOQUE);

        verify(estoque, never()).lerEProcessar(anyString());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("processar - estação sem serviço de handshake → IllegalState (400)")
    void processar_estacaoSemServico_lanca() {
        EstacaoClpHandshake estoque = handshake(EstacaoClp.ESTOQUE);
        ClpComandoService service = new ClpComandoService(List.of(estoque), ipRegistry, publisher);

        assertThatThrownBy(() -> service.processar(EstacaoClp.EXPEDICAO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("processarTodas - despacha em todas as estações com IP")
    void processarTodas_despachaTodas() {
        EstacaoClpHandshake estoque = handshake(EstacaoClp.ESTOQUE);
        EstacaoClpHandshake processo = handshake(EstacaoClp.PROCESSO);
        EstacaoClpHandshake montagem = handshake(EstacaoClp.MONTAGEM);
        EstacaoClpHandshake expedicao = handshake(EstacaoClp.EXPEDICAO);
        ClpComandoService service = new ClpComandoService(
                List.of(estoque, processo, montagem, expedicao), ipRegistry, publisher);
        when(ipRegistry.getIp(any())).thenReturn("10.0.0.1");

        service.processarTodas();

        verify(estoque).lerEProcessar("10.0.0.1");
        verify(processo).lerEProcessar("10.0.0.1");
        verify(montagem).lerEProcessar("10.0.0.1");
        verify(expedicao).lerEProcessar("10.0.0.1");
    }
}
