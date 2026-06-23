package com.tecdes.smart.app_smart_40.service.sse;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint único do canal SSE. N clientes ({@code EventSource}) conectam aqui e recebem todos os
 * eventos transmitidos pelo {@link SseNotifier} via {@link SseEmitterRegistry}.
 *
 * <p>Somente-leitura: este endpoint apenas entrega dados; não dispara escrita no CLP.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final SseEmitterRegistry registry;

    /** Timeout do emitter em ms (o EventSource reconecta sozinho ao expirar). */
    @Value("${clp.sse.timeout:3600000}")
    private long timeoutMs;

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
