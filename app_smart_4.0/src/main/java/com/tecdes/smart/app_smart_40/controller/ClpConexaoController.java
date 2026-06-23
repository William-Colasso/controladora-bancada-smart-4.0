package com.tecdes.smart.app_smart_40.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.smart.app_smart_40.dto.request.ClpIpUpdateRequest;
import com.tecdes.smart.app_smart_40.dto.response.ClpConexaoResponseDTO;
import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.ClpLeituraRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.ClpHealthService;

import lombok.RequiredArgsConstructor;

/**
 * Conexão/desconexão de leitura por estação — lado <b>leitura</b> opt-in do SSE.
 *
 * <p>"Conectar" grava o IP, testa se o CLP responde (S7 :102) e só então habilita a leitura read-only
 * daquela estação; os produtores passam a emitir {@code estacao-status} via SSE. "Desconectar" para a
 * leitura. A leitura nasce desabilitada no boot (ver {@link ClpLeituraRegistry}).
 *
 * <p>Rotas:
 * <ul>
 *   <li>{@code POST /api/clp/{estacao}/conectar} (body {@code {"ip":"x.x.x.x"}}) — grava IP, testa e habilita.</li>
 *   <li>{@code POST /api/clp/{estacao}/desconectar} — desabilita a leitura.</li>
 * </ul>
 * Estação ou IP inválidos → 400 (tratado pelo {@code GlobalExceptionHandler}). CLP inalcançável → 200
 * com {@code alcancavel:false, leitura:false} (a leitura permanece desligada).
 */
@RestController
@RequestMapping("/api/clp")
@RequiredArgsConstructor
public class ClpConexaoController {

    private final ClpIpRegistry ipRegistry;
    private final ClpHealthService healthService;
    private final ClpLeituraRegistry leituraRegistry;

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
