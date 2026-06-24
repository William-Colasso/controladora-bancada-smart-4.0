package com.tecdes.smart.app_smart_40.service.sse;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoAllData;
import com.tecdes.smart.app_smart_40.dto.event.EstacaoStatusEvent;
import com.tecdes.smart.app_smart_40.dto.event.EstoqueGridEvent;
import com.tecdes.smart.app_smart_40.dto.event.ExpedicaoGridEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("SseNotifier")
class SseNotifierTest {

    @Mock
    private SseEmitterRegistry registry;
    @InjectMocks
    private SseNotifier notifier;

    @Test
    @DisplayName("status de estação → broadcast 'estacao-status'")
    void estacao_broadcastNomeCerto() {
        EstacaoStatusEvent e = new EstacaoStatusEvent("estoque", "on", 1);

        notifier.onEstacaoStatus(e);

        verify(registry).broadcast("estacao-status", e);
    }

    @Test
    @DisplayName("dados completos de estação → broadcast 'estacao-all'")
    void estacaoAll_broadcastNomeCerto() {
        EstacaoAllData e = new EstacaoAllData("estoque", null);

        notifier.onEstacaoAll(e);

        verify(registry).broadcast("estacao-all", e);
    }

    @Test
    @DisplayName("grid de estoque → broadcast 'estoque'")
    void estoque_broadcastNomeCerto() {
        EstoqueGridEvent e = new EstoqueGridEvent(emptyList());

        notifier.onEstoqueGrid(e);

        verify(registry).broadcast("estoque", e);
    }

    @Test
    @DisplayName("grid de expedição → broadcast 'expedicao'")
    void expedicao_broadcastNomeCerto() {
        ExpedicaoGridEvent e = new ExpedicaoGridEvent(emptyList());

        notifier.onExpedicaoGrid(e);

        verify(registry).broadcast("expedicao", e);
    }
}
