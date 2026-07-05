package com.tecdes.smart.app_smart_40.dto.response;

/** Configuração da controladora de tampa (ESP32) — request e response de /api/config/tampa. */
public record TampaConfigDTO(boolean habilitada, String ip) {
}
