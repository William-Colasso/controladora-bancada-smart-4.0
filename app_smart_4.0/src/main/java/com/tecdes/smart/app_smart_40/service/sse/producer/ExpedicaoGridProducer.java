package com.tecdes.smart.app_smart_40.service.sse.producer;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.dto.event.ExpedicaoGridEvent;
import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.service.ExpedicaoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Produtor read-only do grid de expedição. Fonte = banco ({@link ExpedicaoService#listarTodos()}, a
 * mesma fonte canônica do {@code GET /api/expedicao}). Publica {@link ExpedicaoGridEvent} só quando muda.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpedicaoGridProducer {

    private final ExpedicaoService expedicaoService;
    private final ApplicationEventPublisher publisher;

    private List<ExpedicaoResponseDTO> ultimo;

    @Scheduled(fixedDelayString = "${clp.grid.interval:2000}")
    public void poll() {
        try {
            List<ExpedicaoResponseDTO> atual = expedicaoService.listarTodos();
            if (!atual.equals(ultimo)) {
                System.out.println(atual.equals(ultimo));
                ultimo = atual;
                publisher.publishEvent(new ExpedicaoGridEvent(atual));
            }
        } catch (Exception e) {
            log.error("Falha ao capturar grid de expedição: {}", e.getMessage());
        }
    }
}
