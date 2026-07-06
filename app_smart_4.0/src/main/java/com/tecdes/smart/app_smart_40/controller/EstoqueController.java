package com.tecdes.smart.app_smart_40.controller;

import com.tecdes.smart.app_smart_40.dto.request.EstoqueRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Estoque", description = "As 28 posições do magazine de blocos. `corBloco`: 0=VAZIO · 1=PRETO · 2=VERMELHO · 3=AZUL. Mutações disparam o evento SSE `estoque`.")
@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @Operation(summary = "Posições ocupadas",
            description = "Só as posições com bloco (corBloco ≠ 0). Usado pelo formulário para validar disponibilidade.")
    @GetMapping("/disponivel")
    public ResponseEntity<List<EstoqueResponseDTO>> getDisponivel() {
        return ResponseEntity.ok(estoqueService.getDisponivel());
    }

    @Operation(summary = "Define a cor de uma posição",
            description = "Grava a cor na posição (aceita 0 = esvaziar — é também o jeito de limpar uma posição). Página magazine usa este endpoint para tudo.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(name = "Bloco vermelho na posição 5", value = "{ \"posicao\": 5, \"corBloco\": 2 }"))))
    @PutMapping("/adicionar")
    public ResponseEntity<EstoqueResponseDTO> adicionarBloco(@RequestBody EstoqueRequestDTO dto) {
        return ResponseEntity.ok(estoqueService.adicionarBloco(dto));
    }

    @Operation(summary = "Esvazia uma posição",
            description = "Seta corBloco = 0 na posição (1–28). Posição inexistente → 404.")
    @PutMapping("/remover/{nrPosicao}")
    public ResponseEntity<EstoqueResponseDTO> removerBloco(@PathVariable Byte nrPosicao) {
        return ResponseEntity.ok(estoqueService.removerBloco(nrPosicao));
    }

    /**
     * Todas as 28 posições, independente da cor.
     * Usado pelo dashboard para renderizar o grid completo.
     */
    @Operation(summary = "Todas as 28 posições",
            description = "Grid completo, incluindo vazias. Usado pelo dashboard.")
    @GetMapping("")
    public ResponseEntity<List<EstoqueResponseDTO>> getTodos() {
        return ResponseEntity.ok(estoqueService.getTodos());
    }

}
