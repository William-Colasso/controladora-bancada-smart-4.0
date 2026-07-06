package com.tecdes.smart.app_smart_40.controller;

import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.dto.response.PedidoResponseDTO;
import com.tecdes.smart.app_smart_40.service.ExpedicaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ExpedicaoController
 *
 * Endpoints REST para o módulo de expedição.
 * Consumido diretamente pelo dashboard.js via polling.
 *
 * Rotas:
 * GET /api/expedicao — lista todas as posições de expedição
 * POST /api/expedicao — registra um pedido na expedição
 * GET /api/expedicao/livre — verifica se há posição livre
 */
@RestController
@RequestMapping("/api/expedicao")
@RequiredArgsConstructor
public class ExpedicaoController {

    private final ExpedicaoService expedicaoService;

    /**
     * Lista todas as posições de expedição (com ou sem pedido).
     * Chamado pelo polling do dashboard.js a cada 3 segundos.
     */
    @GetMapping
    public ResponseEntity<List<ExpedicaoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(expedicaoService.listarTodos());
    }

    /**
     * Registra um pedido em uma posição de expedição.
     * 
     * @PostMapping
     *              public ResponseEntity<ExpedicaoResponseDTO>
     *              registrar(@RequestBody ExpedicaoRequestDTO dto) {
     *              return
     *              ResponseEntity.ok(expedicaoService.atualizarExpedicao(dto));
     *              }
     */
    /**
     * Retorna true se houver pelo menos uma posição de expedição livre.
     * Útil para validação antes de criar um pedido.
     */
    @GetMapping("/livre")
    public ResponseEntity<Boolean> posicaoLivre() {
        return ResponseEntity.ok(expedicaoService.existePosicaoLivre());
    }

    /** Limpa manualmente a posição (banco + zera a OP no magazine do CLP). Página magazine. */
    @DeleteMapping("/{posicao}")
    public ResponseEntity<Void> limparPosicao(@PathVariable int posicao) {
        expedicaoService.limparPosicao(posicao);
        return ResponseEntity.noContent().build();
    }

    /** Histórico: todos os pedidos que já passaram pela posição (mais recente primeiro). */
    @GetMapping("/{posicao}/pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> historico(@PathVariable int posicao) {
        return ResponseEntity.ok(expedicaoService.historicoDaPosicao(posicao));
    }
}
