package com.tecdes.smart.app_smart_40.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.smart.app_smart_40.dto.request.ClpPollingUpdateRequest;
import com.tecdes.smart.app_smart_40.dto.response.ClpPollingResponseDTO;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.clp.ClpPollingRegistry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Configuração em tempo de execução do intervalo de polling do CLP de cada estação.
 * Intervalo inválido ({@code <= 0}) ou estação inválida resultam em 400 (tratado pelo
 * {@code GlobalExceptionHandler}).
 */
@Tag(name = "CLP — Polling", description = "Intervalo de polling (ms) do CLP de cada estação, mutável em runtime (sem restart — o scheduler resolve o intervalo a cada tick). Estações: `estoque`, `processo`, `montagem`, `expedicao`.")
@RestController
@RequestMapping("/api/clp/polling")
@RequiredArgsConstructor
public class ClpPollingController {

    private final ClpPollingRegistry pollingRegistry;

    @Operation(summary = "Intervalo de polling atual de cada estação")
    @GetMapping
    public ResponseEntity<List<ClpPollingResponseDTO>> listar() {
        List<ClpPollingResponseDTO> lista = pollingRegistry.snapshot().entrySet().stream()
                .map(e -> new ClpPollingResponseDTO(e.getKey().apiName(), e.getValue()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Define o intervalo de polling de uma estação",
            description = "Valida o intervalo (deve ser > 0 ms; inválido → 400). Efeito imediato no próximo tick do scheduler.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = "{ \"intervaloMs\": 300 }"))))
    @PutMapping("/{estacao}")
    public ResponseEntity<ClpPollingResponseDTO> atualizar(
            @Parameter(description = "estoque | processo | montagem | expedicao") @PathVariable String estacao,
            @RequestBody ClpPollingUpdateRequest req) {
        EstacoesCLP alvo = EstacoesCLP.fromApi(estacao);
        long intervalo = pollingRegistry.setIntervalo(alvo, req.intervaloMs());
        return ResponseEntity.ok(new ClpPollingResponseDTO(alvo.apiName(), intervalo));
    }
}
