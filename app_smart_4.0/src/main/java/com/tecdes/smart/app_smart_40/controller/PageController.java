package com.tecdes.smart.app_smart_40.controller;

import com.tecdes.smart.app_smart_40.model.enums.*;
import com.tecdes.smart.app_smart_40.service.EstoqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final EstoqueService estoqueService;

    @GetMapping("/formulario")
    public String formulario(Model model) {

        // ── Cores de bloco disponíveis no estoque (sem VAZIO) ──────────────
        List<CorBloco> coresBlocos = estoqueService.getDisponivel()
                .stream()
                .map(e -> e.corBloco())
                .distinct()
                .filter(c -> c != CorBloco.VAZIO)
                .toList();

        model.addAttribute("coresBlocos", coresBlocos);

        // ── Enums para os selects estáticos do pedido ──────────────────────
        model.addAttribute("tiposPedido",  TipoPedido.values());
        model.addAttribute("coresTampa",   CorTampa.values());

        // ── Dados para os selects de lâmina (injetados via Thymeleaf inline) ─
        // Formato: List<Map<String,Object>> → { nome, valor }
        List<Map<String, Object>> coresLaminas = Arrays.stream(CorLamina.values())
                .map(c -> Map.<String, Object>of("nome", c.name(), "valor", c.getValue()))
                .toList();

        List<Map<String, Object>> padroes = Arrays.stream(PadraoLamina.values())
                .map(p -> Map.<String, Object>of("nome", p.name(), "valor", p.getValue()))
                .toList();

        List<Map<String, Object>> posicoes = Arrays.stream(PosicaoLamina.values())
                .map(p -> Map.<String, Object>of("nome", p.name(), "valor", p.getValue()))
                .toList();

        model.addAttribute("coresLaminas", coresLaminas);
        model.addAttribute("padroes",      padroes);
        model.addAttribute("posicoes",     posicoes);

        return "formulario";
    }
}