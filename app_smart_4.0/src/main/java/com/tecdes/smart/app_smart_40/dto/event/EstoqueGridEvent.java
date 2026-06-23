package com.tecdes.smart.app_smart_40.dto.event;

import java.util.List;

import com.tecdes.smart.app_smart_40.dto.response.EstoqueResponseDTO;

/**
 * Evento com o grid completo de estoque (as 28 posições) para push em tempo real.
 *
 * <p>Mesmo payload que {@code GET /api/estoque} já retorna — o frontend consome igual, só que via SSE.
 */
public record EstoqueGridEvent(List<EstoqueResponseDTO> posicoes) {
}
