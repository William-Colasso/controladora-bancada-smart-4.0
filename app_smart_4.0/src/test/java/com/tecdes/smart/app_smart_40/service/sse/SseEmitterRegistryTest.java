package com.tecdes.smart.app_smart_40.service.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoHeartbeat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SseEmitterRegistry")
class SseEmitterRegistryTest {

    @Mock
    private SseEmitter bom;
    @Mock
    private SseEmitter morto;

    @Test
    @DisplayName("broadcast - cliente que falha é removido e não interrompe os demais")
    void broadcast_clienteMortoNaoAfetaOutros() throws Exception {
        doThrow(new IOException("conexão morta"))
                .when(morto).send(any(SseEmitter.SseEventBuilder.class));

        SseEmitterRegistry registry = new SseEmitterRegistry();
        registry.add(bom);
        registry.add(morto);

        registry.broadcast("estoque", "payload");

        verify(bom).send(any(SseEmitter.SseEventBuilder.class)); // o bom recebeu mesmo com o outro falhando
        assertThat(registry.count()).isEqualTo(1);               // o morto foi removido
    }

    @Test
    @DisplayName("broadcast - após remover o morto, próximo envio só vai ao cliente vivo")
    void broadcast_aposRemocao_soClienteVivo() throws Exception {
        doThrow(new IOException("conexão morta"))
                .when(morto).send(any(SseEmitter.SseEventBuilder.class));

        SseEmitterRegistry registry = new SseEmitterRegistry();
        registry.add(bom);
        registry.add(morto);

        registry.broadcast("estoque", "primeiro");
        registry.broadcast("estoque", "segundo");

        verify(bom, org.mockito.Mockito.times(2)).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(registry.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("broadcastEfemero - envia ao cliente atual mas NÃO cacheia p/ replay no connect")
    void broadcastEfemero_naoReplaya() throws Exception {
        SseEmitterRegistry registry = new SseEmitterRegistry();
        registry.add(bom);

        registry.broadcastEfemero("estacao-heartbeat", new EstacaoHeartbeat("estoque"));
        verify(bom).send(any(SseEmitter.SseEventBuilder.class)); // recebeu o pulso

        registry.add(morto); // replay no connect: nada foi cacheado → nada é reenviado
        verify(morto, never()).send(any(SseEmitter.SseEventBuilder.class));
    }
}
