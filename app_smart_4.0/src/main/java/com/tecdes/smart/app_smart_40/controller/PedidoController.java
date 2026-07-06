package com.tecdes.smart.app_smart_40.controller;

import com.tecdes.smart.app_smart_40.service.clp.PedidoConsumerList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tecdes.smart.app_smart_40.dto.request.PedidoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.PedidoResponseDTO;
import com.tecdes.smart.app_smart_40.service.PedidoService;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/pedidos")
@AllArgsConstructor
public class PedidoController {

    private final PedidoConsumerList pedidoConsumerList;
    private final PedidoService pedidoService;

    // GET /api/pedidos
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    // GET /api/pedidos/fila — ids na ordem da fila de produção (head = em produção).
    @GetMapping("/fila")
    public ResponseEntity<List<Long>> fila() {
        return ResponseEntity.ok(pedidoConsumerList.filaAtual());
    }

    // POST /api/pedidos
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(@RequestBody PedidoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(dto));
    }

    // PUT /api/pedidos/{id} — edita um pedido ainda PENDENTE
    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> atualizar(@PathVariable Long id,
            @RequestBody PedidoRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.atualizar(id, dto));
    }

    // PUT /api/pedidos/{id}/status
    @PutMapping("/{id}/status")
    public ResponseEntity<PedidoResponseDTO> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.concluir(id));
    }

    @PostMapping("/{id}")
    public ResponseEntity<String> enviarParaProducao(@PathVariable Long id) {

        pedidoConsumerList.addOrder(id);

        return ResponseEntity.status(201).body(new String("OK")); // todo
    }

}
