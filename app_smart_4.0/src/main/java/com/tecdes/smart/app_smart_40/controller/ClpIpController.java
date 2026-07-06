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
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Configuração em tempo de execução do IP do CLP de cada estação.
 * IP ou estação inválidos resultam em 400 (tratado pelo {@code GlobalExceptionHandler}).
 */
@Tag(name = "CLP — IPs", description = "IP do CLP de cada estação, mutável em runtime (sem restart — os leitores resolvem o IP a cada ciclo). Estações: `estoque`, `processo`, `montagem`, `expedicao`.")
@RestController
@RequestMapping("/api/clp/ips")
@RequiredArgsConstructor
public class ClpIpController {

    private final ClpIpRegistry ipRegistry;

    @Operation(summary = "IP atual de cada estação")
    @GetMapping
    public ResponseEntity<List<ClpIpResponseDTO>> listar() {
        List<ClpIpResponseDTO> lista = ipRegistry.snapshot().entrySet().stream()
                .map(e -> new ClpIpResponseDTO(e.getKey().apiName(), e.getValue()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Define o IP de uma estação",
            description = "Valida o IPv4 (inválido → 400) e derruba a conexão antiga se nenhuma outra estação a usa. Efeito imediato no próximo ciclo de leitura.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = "{ \"ip\": \"10.74.241.10\" }"))))
    @PutMapping("/{estacao}")
    public ResponseEntity<ClpIpResponseDTO> atualizar(
            @Parameter(description = "estoque | processo | montagem | expedicao") @PathVariable String estacao,
            @RequestBody ClpIpUpdateRequest req) {
        EstacoesCLP alvo = EstacoesCLP.fromApi(estacao);
        String ip = ipRegistry.setIp(alvo, req.ip());
        return ResponseEntity.ok(new ClpIpResponseDTO(alvo.apiName(), ip));
    }
}
