package com.tecdes.smart.app_smart_40.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.smart.app_smart_40.dto.request.ClpIpUpdateRequest;
import com.tecdes.smart.app_smart_40.dto.response.ClpIpResponseDTO;
import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;

import lombok.RequiredArgsConstructor;

/**
 * Configuração em tempo de execução do IP do CLP de cada estação.
 *
 * <p>Rotas:
 * <ul>
 *   <li>{@code GET /api/clp/ips} — lista o IP atual de cada estação.</li>
 *   <li>{@code PUT /api/clp/ips/{estacao}} — define o IP de uma estação (estoque/processo/montagem/expedicao).</li>
 * </ul>
 * IP ou estação inválidos resultam em 400 (tratado pelo {@code GlobalExceptionHandler}).
 */
@RestController
@RequestMapping("/api/clp/ips")
@RequiredArgsConstructor
public class ClpIpController {

    private final ClpIpRegistry ipRegistry;

    @GetMapping
    public ResponseEntity<List<ClpIpResponseDTO>> listar() {
        List<ClpIpResponseDTO> lista = ipRegistry.snapshot().entrySet().stream()
                .map(e -> new ClpIpResponseDTO(e.getKey().apiName(), e.getValue()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{estacao}")
    public ResponseEntity<ClpIpResponseDTO> atualizar(
            @PathVariable String estacao,
            @RequestBody ClpIpUpdateRequest req) {
        EstacaoClp alvo = EstacaoClp.fromApi(estacao);
        String ip = ipRegistry.setIp(alvo, req.ip());
        return ResponseEntity.ok(new ClpIpResponseDTO(alvo.apiName(), ip));
    }
}
