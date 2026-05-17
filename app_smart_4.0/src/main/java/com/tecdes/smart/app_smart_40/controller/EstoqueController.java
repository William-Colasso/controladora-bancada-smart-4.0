package com.tecdes.smart.app_smart_40.controller;


import com.tecdes.smart.app_smart_40.dto.EstoqueDTO;
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
    public ResponseEntity<List<EstoqueDTO>> getDisponivel() {
        return ResponseEntity.ok(estoqueService.getDisponivel());
    }

    @PutMapping("/adicionar")
    public ResponseEntity<EstoqueDTO> adicionarBloco(@RequestBody EstoqueDTO dto) {
        return ResponseEntity.ok(estoqueService.adicionarBloco(dto));
    }

    @PutMapping("/remover/{nrPosicao}")
    public ResponseEntity<EstoqueDTO> removerBloco(@PathVariable Byte nrPosicao) {
        return ResponseEntity.ok(estoqueService.removerBloco(nrPosicao));
    }
}