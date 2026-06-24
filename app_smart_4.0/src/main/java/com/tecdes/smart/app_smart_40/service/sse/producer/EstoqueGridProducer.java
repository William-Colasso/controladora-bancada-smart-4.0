package com.tecdes.smart.app_smart_40.service.sse.producer;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.dto.event.EstoqueGridEvent;
import com.tecdes.smart.app_smart_40.dto.response.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.service.EstoqueService;
import com.tecdes.smart.app_smart_40.service.sse.SseEmitterRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Produtor read-only do grid de estoque. Fonte = banco ({@link EstoqueService#getTodos()}, a mesma
 * fonte canônica do {@code GET /api/estoque}). Publica {@link EstoqueGridEvent} só quando muda.
 *
 * <p>Só consulta o banco quando há ao menos um cliente SSE conectado
 * ({@link SseEmitterRegistry#count()} &gt; 0) — sem ninguém ouvindo, o {@code poll()} retorna cedo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EstoqueGridProducer {

    private final EstoqueService estoqueService;
    private final ApplicationEventPublisher publisher;
    private final SseEmitterRegistry sseRegistry;

    private List<EstoqueResponseDTO> ultimo;

    @Scheduled(fixedDelayString = "${clp.grid.interval:2000}")
    public void poll() {
        if (sseRegistry.count() == 0) {
            ultimo = null; // sem clientes → não consulta o banco; ao reconectar, reemite
            return;
        }
        try {
            List<EstoqueResponseDTO> atual = estoqueService.getTodos();
            if (!atual.equals(ultimo)) {
                ultimo = atual;
                publisher.publishEvent(new EstoqueGridEvent(atual));
            }
        } catch (Exception e) {
            log.error("Falha ao capturar grid de estoque: {}", e.getMessage());
        }
    }
}
