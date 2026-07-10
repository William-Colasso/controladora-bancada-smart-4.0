package com.tecdes.smart.app_smart_40.controller;

import com.tecdes.smart.app_smart_40.dto.response.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.dto.response.PedidoResponseDTO;
import com.tecdes.smart.app_smart_40.model.enums.*;
import com.tecdes.smart.app_smart_40.service.EstoqueService;
import com.tecdes.smart.app_smart_40.service.ExpedicaoService;
import com.tecdes.smart.app_smart_40.service.PedidoService;
import com.tecdes.smart.app_smart_40.service.clp.TampaConfigRegistry;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.JsonNodeException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Controller
public class PageController {

        private final EstoqueService estoqueService;
        private final ExpedicaoService expedicaoService;
        private final PedidoService pedidoService;
        private final ObjectMapper objectMapper;
        private final TampaConfigRegistry tampaConfigRegistry;

        /**
         * GET /
         * Tela inicial: ponto de entrada da aplicação. Apenas navegação para as
         * demais telas + painel (vazio por enquanto) de status da bancada.
         */
        @GetMapping("/")
        public String home() {
                return "home";
        }

        @GetMapping("/formulario")
        public String formulario(Model model, @RequestParam(required = false) Long id) {

                // Modo edição (id presente): carrega o pedido e oferece todas as cores (menos
                // VAZIO)
                // para que a cor atual do pedido apareça mesmo com o estoque dela já zerado.
                String pedidoEditJson = null;
                List<CorBloco> coresBlocos;
                if (id != null) {
                        PedidoResponseDTO pedido = pedidoService.buscarPorId(id);
                        pedidoEditJson = toJson(pedido);
                        coresBlocos = Arrays.stream(CorBloco.values())
                                        .filter(c -> c != CorBloco.VAZIO)
                                        .toList();
                } else {
                        // ── Cores de bloco disponíveis no estoque (sem VAZIO) ──────────────
                        coresBlocos = estoqueService.getDisponivel()
                                        .stream()
                                        .map(e -> e.corBloco())
                                        .distinct()
                                        .filter(c -> c != CorBloco.VAZIO)
                                        .toList();
                }

                model.addAttribute("coresBlocos", coresBlocos);
                model.addAttribute("pedidoEditJson", pedidoEditJson);

                // Sugestão de OP para o modo criação (editável). Em edição, a OP vem no
                // pedidoEditJson e o JS prefila o campo.
                model.addAttribute("proximaOrdem", pedidoService.proximaOrdemProducao());

                // ── Enums para os selects estáticos do pedido ──────────────────────
                model.addAttribute("tiposPedido", TipoPedido.values());
                model.addAttribute("coresTampa", CorTampa.values());


                model.addAttribute("tampaHabilitada", tampaConfigRegistry.isHabilitada());
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
                model.addAttribute("padroes", padroes);
                model.addAttribute("posicoes", posicoes);

                return "formulario";
        }

        /**
         * GET /pedidos
         * Retorna o template HTML. Nenhum model attribute necessário.
         */
        @GetMapping("/pedidos")
        public String pedidos() {
                return "pedidos/pedidos";
        }

        /**
         * GET /estacoes
         * Status ao vivo das 4 estações — consumidor SSE puro ({@code estacao-status}).
         * Sem service/model: a tela lê tudo do CLP via SSE, não acessa o banco.
         */
        @GetMapping("/estacoes")
        public String estacoes() {
                return "estacoes/estacoes";
        }

        /**
         * GET /magazine
         * Controle manual dos magazines: estoque (28 posições, cor) e expedição
         * (12 posições, limpar). Dados iniciais SSR; ao vivo via SSE.
         */
        @GetMapping("/magazine")
        public String magazine(Model model) {
                model.addAttribute("estoqueJson", toJson(estoqueService.getTodos()));
                model.addAttribute("expedicaoJson", toJson(expedicaoService.listarTodos()));
                return "magazine/magazine";
        }

        /**
         * GET /configuracao
         * Configuração da comunicação: IPs dos CLPs (base + final por estação) e
         * controladora de tampa (ESP32). Sem model — tudo via /api/clp/ips e /api/config/tampa.
         */
        @GetMapping("/configuracao")
        public String configuracao() {
                return "configuracao/configuracao";
        }

        /**
         * GET /Dashboard
         * Exibe o dashboard com dados iniciais de estoque e expedição.
         */
        @GetMapping("/dashboard")
        public String dashboard(Model model) {

                // ---- Estoque ----
                List<EstoqueResponseDTO> estoque = estoqueService.getTodos();

                Map<String, Integer> estoqueStats = new java.util.HashMap<>();
                for (CorBloco cor : CorBloco.values()) {
                        Long count = estoque.stream()
                                        .filter(e -> e.corBloco() == cor)
                                        .count();
                        estoqueStats.put(cor.name(), count.intValue());
                }
                estoqueStats.entrySet()
                                .stream()
                                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                Map.Entry::getValue,
                                                (a, b) -> a,
                                                LinkedHashMap::new));

                // ---- Expedição ----
                List<ExpedicaoResponseDTO> expedicao = expedicaoService.listarTodos();
                long expOcupadas = expedicao.stream()
                                .filter(e -> e.pedidoResponseDTO() != null)
                                .count();

                // ---- Serialização para JSON (dados iniciais no hidden input) ----
                String estoqueJson = toJson(estoque);
                String expedicaoJson = toJson(expedicao);

                /// ----- Loggs

                // ---- Model ----
                model.addAttribute("estoqueJson", estoqueJson);
                model.addAttribute("expedicaoJson", expedicaoJson);
                model.addAttribute("estoqueStats", estoqueStats);
                model.addAttribute("expedicaoOcupadas", expOcupadas);

                return "dashboard/dashboard";
        }

        // -------------------------------------------------------------------------
        // Helpers
        // -------------------------------------------------------------------------

        private String toJson(Object obj) {
                try {
                        return objectMapper.writeValueAsString(obj);
                } catch (JsonNodeException e) {
                        log.warn("Falha ao serializar objeto para JSON: {}", e.getMessage());
                        return "[]";
                }
        }
}