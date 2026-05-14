package com.tecdes.smart.app_smart_40.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tecdes.smart.app_smart_40.dto.PedidoDTO;
import com.tecdes.smart.app_smart_40.service.PedidoService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@AllArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    // GET /api/pedidos
    @GetMapping
    public ResponseEntity<List<PedidoDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    // POST /api/pedidos
    @PostMapping
    public ResponseEntity<PedidoDTO> criar(@RequestBody PedidoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(dto));
    }

    // PUT /api/pedidos/{id}/status
    @PutMapping("/{id}/status")
    public ResponseEntity<PedidoDTO> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.concluir(id));
    }

}