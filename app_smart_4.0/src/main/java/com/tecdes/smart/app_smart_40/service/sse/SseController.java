package com.tecdes.smart.app_smart_40.service.sse;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint único do canal SSE. N clientes ({@code EventSource}) conectam aqui e recebem todos os
 * eventos transmitidos pelo {@link SseNotifier} via {@link SseEmitterRegistry}.
 *
 * <p>Somente-leitura: este endpoint apenas entrega dados; não dispara escrita no CLP.
 */
@Tag(name = "Stream (SSE)", description = "Canal de tempo real — abra com `new EventSource('/api/stream')`. Conectar também liga a leitura automática dos CLPs (parada quando o último cliente sai).")
@RestController
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final SseEmitterRegistry registry;

    /** Timeout do emitter em ms (o EventSource reconecta sozinho ao expirar). */
    @Value("${clp.sse.timeout:3600000}")
    private long timeoutMs;

    @Operation(summary = "Conecta ao stream de eventos",
            description = """
                    `text/event-stream`. Ao conectar, recebe `conectado` + replay do último snapshot de cada evento cacheado. Eventos:

                    | Evento | Payload | Quando |
                    |---|---|---|
                    | `estacao-status` | `{estacao, estado, funcionamento}` | status derivado da estação mudou |
                    | `estacao-all` | `{estacao, dados}` (bean completo do CLP) | qualquer variável da estação mudou |
                    | `estacao-heartbeat` | `{estacao}` | a cada passada de leitura (liveness; sem pulso >2.5s = sem comunicação) |
                    | `estoque` | lista das 28 posições | grid de estoque mudou no banco |
                    | `expedicao` | lista das 12 posições | grid de expedição mudou no banco |""")
    @GetMapping(value = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        registry.add(emitter);
        try {
            emitter.send(SseEmitter.event().name("conectado").data("ok"));
        } catch (IOException e) {
            log.debug("Cliente SSE desconectou antes do handshake: {}", e.getMessage());
        }
        return emitter;
    }
}
