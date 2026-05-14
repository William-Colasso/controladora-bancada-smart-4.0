package com.tecdes.smart.app_smart_40.controller;

import com.tecdes.smart.app_smart_40.dto.EstoqueRequestDTO;
import com.tecdes.smart.app_smart_40.dto.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.service.EstoqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping("/disponivel")
    public ResponseEntity<List<EstoqueResponseDTO>> getDisponivel() {
        return ResponseEntity.ok(estoqueService.getDisponivel());
    }

    @PutMapping("/adicionar")
    public ResponseEntity<EstoqueResponseDTO> adicionarBloco(@RequestBody EstoqueRequestDTO dto) {
        return ResponseEntity.ok(estoqueService.adicionarBloco(dto));
    }

    @PutMapping("/remover/{nrPosicao}")
    public ResponseEntity<EstoqueResponseDTO> removerBloco(@PathVariable Byte nrPosicao) {
        return ResponseEntity.ok(estoqueService.removerBloco(nrPosicao));
    }
}