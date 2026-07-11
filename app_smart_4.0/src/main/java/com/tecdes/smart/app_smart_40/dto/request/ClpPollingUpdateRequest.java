package com.tecdes.smart.app_smart_40.dto.request;

/** Corpo do {@code PUT /api/clp/polling/{estacao}}: novo intervalo de polling (ms) daquela estação. */
public record ClpPollingUpdateRequest(Long intervaloMs) {
}
