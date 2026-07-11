package com.tecdes.smart.app_smart_40.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.smart.app_smart_40.service.clp.EstadoProducaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Liga/desliga o modo somente-leitura do CLP (todas as estações). Quando ligado, as estações leem o
 * PLC mas não escrevem flags de volta (o handshake pula as escritas, ver {@code processarHandshake}).
 */
@Tag(name = "CLP — modo somente-leitura", description = "Flag singleton global: leitura+escrita (padrão) ou somente-leitura.")
@RestController
@RequestMapping("/api/clp/somente-leitura")
@RequiredArgsConstructor
public class ClpModoLeituraController {

    private final EstadoProducaoService estado;

    @Operation(summary = "Estado atual do modo somente-leitura")
    @GetMapping
    public ResponseEntity<Map<String, Boolean>> obter() {
        return ResponseEntity.ok(Map.of("readOnly", estado.isReadOnly()));
    }

    @Operation(summary = "Liga (ativo=true → somente-leitura) ou desliga (leitura+escrita)")
    @PutMapping
    public ResponseEntity<Map<String, Boolean>> definir(@RequestParam boolean ativo) {
        estado.setReadOnly(ativo);
        return ResponseEntity.ok(Map.of("readOnly", ativo));
    }
}
