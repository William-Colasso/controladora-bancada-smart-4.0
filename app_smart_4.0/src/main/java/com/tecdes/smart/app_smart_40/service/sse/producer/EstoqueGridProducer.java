package com.tecdes.smart.app_smart_40.service.sse.producer;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.dto.event.EstoqueGridEvent;
import com.tecdes.smart.app_smart_40.dto.response.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.service.EstoqueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Produtor read-only do grid de estoque. Fonte = banco ({@link EstoqueService#getTodos()}, a mesma
 * fonte canônica do {@code GET /api/estoque}). Publica {@link EstoqueGridEvent} só quando muda.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EstoqueGridProducer {

    private final EstoqueService estoqueService;
    private final ApplicationEventPublisher publisher;

    private List<EstoqueResponseDTO> ultimo;

    @Scheduled(fixedDelayString = "${clp.grid.interval:2000}")
    public void poll() {
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
