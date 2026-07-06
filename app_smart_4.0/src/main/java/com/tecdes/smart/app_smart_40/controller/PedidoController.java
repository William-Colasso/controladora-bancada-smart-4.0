package com.tecdes.smart.app_smart_40.controller;

import com.tecdes.smart.app_smart_40.service.clp.PedidoConsumerList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tecdes.smart.app_smart_40.dto.request.PedidoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.PedidoResponseDTO;
import com.tecdes.smart.app_smart_40.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Pedidos", description = "Ciclo de vida do pedido: criação (PENDENTE) → fila → produção (PRODUCAO) → expedição (CONCLUIDO). Enums em int — ver legenda no topo.")
@RestController
@RequestMapping("/api/pedidos")
@AllArgsConstructor
public class PedidoController {

    /** Exemplo de corpo de pedido (DUPLO = 2 blocos; enums como int). */
    static final String EXEMPLO_PEDIDO = """
            {
              "tipoPedido": 2,
              "corTampa": 1,
              "ordemProducao": 15,
              "blocos": [
                {
                  "cor": 2,
                  "andar": 1,
                  "laminas": [
                    { "cor": 3, "padrao": 1, "posicao": 1 },
                    { "cor": 4, "padrao": 2, "posicao": 2 },
                    { "cor": 6, "padrao": 0, "posicao": 3 }
                  ]
                },
                {
                  "cor": 3,
                  "andar": 2,
                  "laminas": [
                    { "cor": 1, "padrao": 3, "posicao": 2 }
                  ]
                }
              ]
            }""";

    private final PedidoConsumerList pedidoConsumerList;
    private final PedidoService pedidoService;

    @Operation(summary = "Lista todos os pedidos",
            description = "Todos os pedidos com blocos e lâminas aninhados, em qualquer status.")
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @Operation(summary = "Fila de produção",
            description = "Ids dos pedidos na ordem da fila (head = em produção agora). A fila consome um pedido por vez; é recomposta do banco no boot.")
    @GetMapping("/fila")
    public ResponseEntity<List<Long>> fila() {
        return ResponseEntity.ok(pedidoConsumerList.filaAtual());
    }

    @Operation(summary = "Cria um pedido (PENDENTE)",
            description = "Valida tipo × nº de blocos, estoque disponível e lâminas; **não** envia à produção (use `POST /api/pedidos/{id}`). `ordemProducao` é opcional: se omitida, usa MAX+1; se informada, deve ser positiva e única.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(name = "Pedido duplo", value = EXEMPLO_PEDIDO))))
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(@RequestBody PedidoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(dto));
    }

    @Operation(summary = "Edita um pedido PENDENTE",
            description = "Só pedidos PENDENTE (senão 400). Re-roda as validações de criação, substitui os blocos e preserva id/status/dataCriacao. Pode trocar a `ordemProducao` (mesmas regras de unicidade).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(name = "Pedido duplo", value = EXEMPLO_PEDIDO))))
    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> atualizar(@PathVariable Long id,
            @RequestBody PedidoRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.atualizar(id, dto));
    }

    @Operation(summary = "Conclui manualmente (PRODUCAO → CONCLUIDO)",
            description = "Fallback manual — normalmente a conclusão é automática quando o CLP de expedição guarda a OP do pedido. Pedido já CONCLUIDO → 400.")
    @PutMapping("/{id}/status")
    public ResponseEntity<PedidoResponseDTO> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.concluir(id));
    }

    @Operation(summary = "Enfileira para produção",
            description = "**Não** envia direto ao CLP: adiciona o id à fila; o consumidor (a cada ~1s) dispara `enviarParaProducao` quando o pedido chega ao head e nada está em PRODUCAO. Aí reserva posição de expedição, baixa o estoque e grava a OP no CLP de estoque.")
    @PostMapping("/{id}")
    public ResponseEntity<String> enviarParaProducao(@PathVariable Long id) {

        pedidoConsumerList.addOrder(id);

        return ResponseEntity.status(201).body(new String("OK")); // todo
    }

}
