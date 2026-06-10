package com.tecdes.smart.app_smart_40.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tecdes.smart.app_smart_40.dto.request.BlocoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.request.PedidoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.BlocoResponseDTO;
import com.tecdes.smart.app_smart_40.dto.response.PedidoResponseDTO;
import com.tecdes.smart.app_smart_40.model.enums.TipoPedido;
import com.tecdes.smart.app_smart_40.service.PedidoService;
import com.tecdes.smart.app_smart_40.service.SmartService;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/pedidos")
@AllArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final SmartService smartService;


    // GET /api/pedidos
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    // POST /api/pedidos
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(@RequestBody PedidoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(dto));
    }

    // PUT /api/pedidos/{id}/status
    @PutMapping("/{id}/status")
    public ResponseEntity<PedidoResponseDTO> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.concluir(id));
    }

    @PostMapping("/{id}")
    public ResponseEntity<String> enviarParaProducao(@PathVariable Long id) {
        PedidoResponseDTO pedido = pedidoService.buscarPorId(id);
        List<BlocoRequestDTO> blocos = pedido.blocos().stream().map(bloco -> BlocoResponseDTO.toEntity(bloco))
                .map(blocoE -> BlocoRequestDTO.fromEntity(blocoE)).toList();
        
        System.out.println("Pedido mucho loko pra production"+pedido);
        smartService.enviarParaProducao(pedido);
        pedidoService.atualizar(id, new PedidoRequestDTO(pedido.tipoPedido(), pedido.corTampa(), blocos));
        
        return ResponseEntity.status(201).body(new String("OK")); // todo
    }

    @PostMapping("/clp/{ipClp}")
    public ResponseEntity<String> setIpClp(@PathVariable String ipClp) {
        smartService.setIpClp(ipClp);
        
        return ResponseEntity.ok(smartService.getIpClp());
    }
    


}