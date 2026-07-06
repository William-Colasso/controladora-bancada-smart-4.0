package com.tecdes.smart.app_smart_40.dto.event;

import java.util.List;

import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;

/**
 * Evento com o grid completo de expedição (as 12 posições) para push em tempo real.
 *
 * <p>Mesmo payload que {@code GET /api/expedicao} já retorna — o frontend consome igual, só que via SSE.
 */
public record ExpedicaoGridEvent(List<ExpedicaoResponseDTO> posicoes) {
}
