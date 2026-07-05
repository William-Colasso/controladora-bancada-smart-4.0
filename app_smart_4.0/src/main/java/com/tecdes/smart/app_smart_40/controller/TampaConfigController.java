package com.tecdes.smart.app_smart_40.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.smart.app_smart_40.dto.response.TampaConfigDTO;
import com.tecdes.smart.app_smart_40.service.clp.TampaConfigRegistry;

import lombok.RequiredArgsConstructor;

/**
 * Configuração em runtime da controladora de tampa (ESP32).
 *
 * <ul>
 *   <li>{@code GET /api/config/tampa} — estado atual (habilitada + IP).</li>
 *   <li>{@code PUT /api/config/tampa} — atualiza; IP inválido → 400 (GlobalExceptionHandler).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/config/tampa")
@RequiredArgsConstructor
public class TampaConfigController {

    private final TampaConfigRegistry registry;

    @GetMapping
    public ResponseEntity<TampaConfigDTO> obter() {
        return ResponseEntity.ok(new TampaConfigDTO(registry.isHabilitada(), registry.getIp()));
    }

    @PutMapping
    public ResponseEntity<TampaConfigDTO> atualizar(@RequestBody TampaConfigDTO dto) {
        String ip = registry.atualizar(dto.habilitada(), dto.ip());
        return ResponseEntity.ok(new TampaConfigDTO(registry.isHabilitada(), ip));
    }
}
