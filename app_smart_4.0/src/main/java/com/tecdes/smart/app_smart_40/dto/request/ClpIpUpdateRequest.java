package com.tecdes.smart.app_smart_40.dto.request;

/** Corpo do {@code PUT /api/clp/ips/{estacao}}: novo IP do CLP daquela estação. */
public record ClpIpUpdateRequest(String ip) {
}
