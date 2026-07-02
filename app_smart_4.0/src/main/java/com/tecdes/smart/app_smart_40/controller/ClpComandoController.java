package com.tecdes.smart.app_smart_40.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.ClpComandoService;

import lombok.RequiredArgsConstructor;

/**
 * Disparo sob demanda do handshake de escrita com o CLP — lado <b>escrita</b>
 * do loop CLP↔Backend↔Frontend.
 *
 * <p>
 * Cada chamada executa <b>uma</b> passada de leitura+escrita; o caller
 * (frontend, via poller) repete.
 * O reflexo visual do novo estado chega ao frontend pelo SSE
 * ({@code estacao-status}/{@code estoque}/
 * {@code expedicao}) — esta API não devolve o estado derivado, só confirma a
 * passada.
 *
 * <p>
 * Rotas:
 * <ul>
 * <li>{@code POST /api/clp/{estacao}/processar} — uma passada na estação
 * (estoque/processo/montagem/expedicao).</li>
 * <li>{@code POST /api/clp/processar} — uma passada em todas as estações.</li>
 * </ul>
 * Estação inválida → 400 (tratado pelo {@code GlobalExceptionHandler}).
 */
@RestController
@RequestMapping("/api/clp")
@RequiredArgsConstructor
public class ClpComandoController {

    private final ClpComandoService comandoService;

    @PostMapping("/{estacao}/processar")
    public ResponseEntity<Map<String, Object>> processar(@PathVariable String estacao) {
        EstacoesCLP alvo = EstacoesCLP.fromApi(estacao);
        comandoService.processar(alvo);
        return ResponseEntity.ok(Map.of("estacao", alvo.apiName(), "ok", true));
    }

    @PostMapping("/processar")
    public ResponseEntity<Map<String, Object>> processarTodas() {
        comandoService.processarTodas();
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
