package com.tecdes.smart.app_smart_40.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.smart.app_smart_40.dto.response.TampaConfigDTO;
import com.tecdes.smart.app_smart_40.service.clp.TampaConfigRegistry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Configuração em runtime da controladora de tampa (ESP32).
 */
@Tag(name = "Configuração — tampa", description = "ESP32 que seleciona a tampa física. Desabilitada, o envio à produção pula a chamada à tampa silenciosamente.")
@RestController
@RequestMapping("/api/config/tampa")
@RequiredArgsConstructor
public class TampaConfigController {

    private final TampaConfigRegistry registry;

    @Operation(summary = "Estado atual (habilitada + IP)")
    @GetMapping
    public ResponseEntity<TampaConfigDTO> obter() {
        return ResponseEntity.ok(new TampaConfigDTO(registry.isHabilitada(), registry.getIp()));
    }

    @Operation(summary = "Habilita/desabilita e define o IP",
            description = "IP inválido → 400. Efeito imediato, sem restart.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = "{ \"habilitada\": true, \"ip\": \"10.74.241.245\" }"))))
    @PutMapping
    public ResponseEntity<TampaConfigDTO> atualizar(@RequestBody TampaConfigDTO dto) {
        String ip = registry.atualizar(dto.habilitada(), dto.ip());
        return ResponseEntity.ok(new TampaConfigDTO(registry.isHabilitada(), ip));
    }
}
