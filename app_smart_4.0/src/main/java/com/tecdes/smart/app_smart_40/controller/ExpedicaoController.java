package com.tecdes.smart.app_smart_40.controller;

import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.dto.response.PedidoResponseDTO;
import com.tecdes.smart.app_smart_40.service.ExpedicaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST para o módulo de expedição — as 12 posições que recebem
 * pedidos concluídos. Mutações disparam o evento SSE {@code expedicao}.
 */
@Tag(name = "Expedição", description = "As 12 posições de saída. Cada posição pode ter um `pedidoResponseDTO` (atual) ou null (livre). A reserva acontece no envio à produção; a ocupação real, quando o CLP guarda a peça.")
@RestController
@RequestMapping("/api/expedicao")
@RequiredArgsConstructor
public class ExpedicaoController {

    private final ExpedicaoService expedicaoService;

    @Operation(summary = "Todas as 12 posições",
            description = "Grid completo — posições livres vêm com `pedidoResponseDTO: null`.")
    @GetMapping
    public ResponseEntity<List<ExpedicaoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(expedicaoService.listarTodos());
    }

    @Operation(summary = "Há posição livre?",
            description = "`true` se ao menos uma posição está sem pedido. Útil antes de criar/enviar um pedido — sem posição livre o envio à produção falha.")
    @GetMapping("/livre")
    public ResponseEntity<Boolean> posicaoLivre() {
        return ResponseEntity.ok(expedicaoService.existePosicaoLivre());
    }

    @Operation(summary = "Limpa uma posição",
            description = "Desvincula o pedido no banco **e** zera a OP no magazine do CLP de expedição. Usado pelo botão ✕ da página magazine. O histórico da posição é preservado.")
    @DeleteMapping("/{posicao}")
    public ResponseEntity<Void> limparPosicao(@PathVariable int posicao) {
        expedicaoService.limparPosicao(posicao);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Histórico da posição",
            description = "Todos os pedidos que já passaram pela posição, mais recente primeiro (a FK `Pedido.expedicao` persiste após a liberação).")
    @GetMapping("/{posicao}/pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> historico(@PathVariable int posicao) {
        return ResponseEntity.ok(expedicaoService.historicoDaPosicao(posicao));
    }
}
