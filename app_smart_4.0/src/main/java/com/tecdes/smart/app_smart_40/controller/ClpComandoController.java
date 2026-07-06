package com.tecdes.smart.app_smart_40.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.ClpComandoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Disparo sob demanda do handshake com o CLP (leitura+escrita numa passada).
 * Normalmente desnecessário: o {@code ClpProcessamentoScheduler} roda isto a
 * cada ~300ms enquanto houver cliente SSE conectado.
 */
@Tag(name = "CLP — comandos", description = "Dispara manualmente uma passada do handshake (leitura+processamento) numa estação. O estado resultante chega via SSE, não na resposta — esta API só confirma que a passada rodou.")
@RestController
@RequestMapping("/api/clp")
@RequiredArgsConstructor
public class ClpComandoController {

    private final ClpComandoService comandoService;

    @Operation(summary = "Uma passada numa estação",
            description = "Lê o bloco S7 e processa o handshake da estação. Estação inválida → 400. Roda automaticamente a cada ~300ms com cliente SSE aberto — só chame manualmente para depurar.")
    @PostMapping("/{estacao}/processar")
    public ResponseEntity<Map<String, Object>> processar(
            @Parameter(description = "estoque | processo | montagem | expedicao") @PathVariable String estacao) {
        EstacoesCLP alvo = EstacoesCLP.fromApi(estacao);
        comandoService.processar(alvo);
        return ResponseEntity.ok(Map.of("estacao", alvo.apiName(), "ok", true));
    }

    @Operation(summary = "Uma passada em todas as estações")
    @PostMapping("/processar")
    public ResponseEntity<Map<String, Object>> processarTodas() {
        comandoService.processarTodas();
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
