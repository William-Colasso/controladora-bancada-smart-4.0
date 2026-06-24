package com.tecdes.smart.app_smart_40.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.smart.app_smart_40.dto.request.ClpIpUpdateRequest;
import com.tecdes.smart.app_smart_40.dto.response.ClpConexaoResponseDTO;
import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpComandoService;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.ClpLeituraRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.ClpHealthService;

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
    private final ClpIpRegistry ipRegistry;
    private final ClpHealthService healthService;
    private final ClpLeituraRegistry leituraRegistry;

    @PostMapping("/{estacao}/processar")
    public ResponseEntity<Map<String, Object>> processar(@PathVariable String estacao) {
        EstacaoClp alvo = EstacaoClp.fromApi(estacao);
        comandoService.processar(alvo);
        return ResponseEntity.ok(Map.of("estacao", alvo.apiName(), "ok", true));
    }

    @PostMapping("/processar")
    public ResponseEntity<Map<String, Object>> processarTodas() {
        comandoService.processarTodas();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{estacao}/conectar")
    public ResponseEntity<ClpConexaoResponseDTO> conectar(
            @PathVariable String estacao,
            @RequestBody ClpIpUpdateRequest req) {
        EstacaoClp alvo = EstacaoClp.fromApi(estacao);
        String ip = ipRegistry.setIp(alvo, req.ip());

        boolean alcancavel = healthService.alcancavel(ip);
        if (!alcancavel) {
            leituraRegistry.desabilitar(alvo);
            return ResponseEntity.ok(new ClpConexaoResponseDTO(alvo.apiName(), ip, false, false));
        }

        leituraRegistry.habilitar(alvo);
        return ResponseEntity.ok(new ClpConexaoResponseDTO(alvo.apiName(), ip, true, true));
    }

    @PostMapping("/{estacao}/desconectar")
    public ResponseEntity<ClpConexaoResponseDTO> desconectar(@PathVariable String estacao) {
        EstacaoClp alvo = EstacaoClp.fromApi(estacao);
        leituraRegistry.desabilitar(alvo);
        return ResponseEntity.ok(new ClpConexaoResponseDTO(alvo.apiName(), ipRegistry.getIp(alvo), false, false));
    }
}
