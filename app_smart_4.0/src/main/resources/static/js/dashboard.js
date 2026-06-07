/**
 * dashboard.js — Smart 4.0
 * ─────────────────────────────────────────────────────────────
 * Responsabilidades:
 *   - Renderizar o grid de estoque (28 posições, interativo)
 *   - Renderizar o grid de expedição (12 posições, somente leitura)
 *   - Permitir seleção de blocos e alteração de cor via API
 *   - Exibir OP dos pedidos nas posições de expedição
 *   - Polling automático a cada POLL_INTERVAL ms
 *
 * Contrato com o servidor (DTOs reais):
 *   EstoqueResponseDTO  → { id: Long, posicao: int, corBloco: int }
 *   ExpedicaoResponseDTO → { id: Long, posicao: int, pedidoResponseDTO: PedidoResponseDTO | null }
 *   PedidoResponseDTO   → { id, ordemProducao, status, tipoPedido, corTampa, ... }
 *
 *   ATENÇÃO: corBloco chega como INTEIRO (via @JsonValue do enum):
 *     0 = VAZIO | 1 = PRETO | 2 = VERMELHO | 3 = AZUL
 *
 * Dependências:
 *   - smart40-utils.js (Api, Toast, Utils) — deve ser carregado antes
 *   - Elementos HTML com IDs conforme dashboard.html
 * ─────────────────────────────────────────────────────────────
 */

document.addEventListener("DOMContentLoaded", () => {
  /* ================================================================
     CONSTANTES
     ================================================================ */

  const POLL_INTERVAL = 3000; // ms entre cada atualização automática
  const ESTOQUE_TOTAL = 28; // posições fixas do estoque
  const EXPEDICAO_TOTAL = 12; // posições fixas da expedição

  /**
   * Mapeamento inteiro → nome do enum CorBloco
   * O servidor envia @JsonValue (int), mas a API de mutação
   * recebe o nome string ("PRETO", "VERMELHO", etc.)
   */
  const COR_INT_TO_NAME = {
    0: "VAZIO",
    1: "PRETO",
    2: "VERMELHO",
    3: "AZUL",
  };

  /**
   * Mapeamento nome do enum → classe CSS BEM do bloco
   */
  const COR_NAME_TO_CLASS = {
    VAZIO: "bloco--vazio",
    PRETO: "bloco--preto",
    VERMELHO: "bloco--vermelho",
    AZUL: "bloco--azul",
  };

  /* ================================================================
     ESTADO LOCAL
     ================================================================ */

  const state = {
    estoque: [], // Array<EstoqueResponseDTO>
    expedicao: [], // Array<ExpedicaoResponseDTO>
    selectedPos: new Set(), // posições do estoque selecionadas pelo usuário
    activeColor: null, // string: 'PRETO' | 'VERMELHO' | 'AZUL' | 'VAZIO' | null
    isApplying: false, // guard para não fazer polling durante mutação
    isLoading: true, // primeiro carregamento
  };

  /* ================================================================
     SELETORES DOM
     ================================================================ */

  const estoqueGrid = document.getElementById("estoqueGrid");
  const expedicaoGrid = document.getElementById("expedicaoGrid");
  const applyBtn = document.getElementById("applyColorBtn");
  const selectionInfo = document.getElementById("selectionInfo");
  const colorBtns = document.querySelectorAll(".color-btn[data-color]");

  // Estatísticas do estoque
  const statPreto = document.getElementById("statPreto");
  const statVermelho = document.getElementById("statVermelho");
  const statAzul = document.getElementById("statAzul");
  const statVazio = document.getElementById("statVazio");

  // Ocupação da expedição
  const expOcupado = document.getElementById("expOcupado");
  const expTotal = document.getElementById("expTotal");
  const expFill = document.getElementById("expFill");

  // Ocupação do estoque (barra stacked — referência ao id="ocupacaoFill" se existir)
  // O dashboard.html usa quatro divs filhas separadas (.fill-preto etc.)
  // Esse script atualiza o inline style de cada uma.
  const fillPreto = document.querySelector(".fill-preto");
  const fillVermelho = document.querySelector(".fill-vermelho");
  const fillAzul = document.querySelector(".fill-azul");
  const fillVazio = document.querySelector(".fill-vazio");

  /* ================================================================
     BOOTSTRAP
     ================================================================ */

  // Carrega dados iniciais injetados pelo Thymeleaf (evita flash vazio)
  _carregarDadosIniciais();

  // Primeiro fetch real
  _fetchTudo();

  // Polling automático
  setInterval(() => {
    if (!state.isApplying) _fetchTudo();
  }, POLL_INTERVAL);

  /* ================================================================
     DADOS INICIAIS (Thymeleaf SSR)
     ================================================================ */

  function _carregarDadosIniciais() {
    try {
      const estoqueEl = document.getElementById("initialEstoque");
      const expedicaoEl = document.getElementById("initialExpedicao");

      if (estoqueEl?.value) {
        state.estoque = JSON.parse(estoqueEl.value) || [];
        _renderEstoque();
      }
      if (expedicaoEl?.value) {
        state.expedicao = JSON.parse(expedicaoEl.value) || [];
        _renderExpedicao();
      }
    } catch (e) {
      console.warn("[Dashboard] Falha ao carregar dados iniciais SSR:", e);
    }
  }

  /* ================================================================
     FETCH
     ================================================================ */

  async function _fetchTudo() {
    try {
      const [estoque, expedicao] = await Promise.all([
        Api.get("/api/estoque/todos"),
        Api.get("/api/expedicao"),
      ]);

      state.estoque = estoque || [];
      state.expedicao = expedicao || [];
      state.isLoading = false;

      _renderEstoque();
      _renderExpedicao();
    } catch (err) {
      // Polling silencioso — não exibe toast para não poluir a UI
      console.error("[Dashboard] Erro no fetch:", err);
    }
  }

  /* ================================================================
     RENDER — ESTOQUE
     ================================================================ */

  function _renderEstoque() {
    // Mapa posição → dado para acesso O(1)
    const byPos = {};
    state.estoque.forEach((e) => {
      byPos[e.posicao] = e;
    });

    for (let pos = 1; pos <= ESTOQUE_TOTAL; pos++) {
      let cell = document.getElementById(`bloco-est-${pos}`);

      if (!cell) {
        // Célula não existe no DOM — cria do zero (inclui listener de click)
        cell = _criarCelula(pos, "estoque");
        estoqueGrid.appendChild(cell);
      } else if (!cell.dataset.listenerRegistrado) {
        // Célula existe (gerada pelo Thymeleaf) mas ainda sem listener —
        // registra uma única vez usando o flag data-listener-registrado
        // para evitar duplicação a cada ciclo de render/polling
        cell.addEventListener("click", () => _toggleSelecao(pos));
        cell.dataset.listenerRegistrado = "true";
      }

      const dado = byPos[pos];
      const corInt = dado?.corBloco ?? 0;
      const corName = COR_INT_TO_NAME[corInt] ?? "VAZIO";

      _atualizarCelulaEstoque(cell, pos, corName, dado);
    }

    _renderStatsEstoque();
  }

  function _atualizarCelulaEstoque(cell, pos, corName, dado) {
    // Remove todas as classes de cor anteriores
    const corClasses = Object.values(COR_NAME_TO_CLASS);
    cell.classList.remove(...corClasses, "skeleton", "bloco--selected");

    // Aplica a nova cor
    cell.classList.add(COR_NAME_TO_CLASS[corName] || "bloco--vazio");

    // Mantém seleção visual
    if (state.selectedPos.has(pos)) {
      cell.classList.add("bloco--selected");
    }

    // Conteúdo interno
    cell.innerHTML = `<span class="bloco__pos">${pos}</span>`;
  }

  /* ================================================================
     RENDER — EXPEDIÇÃO
     ================================================================ */

  function _renderExpedicao() {
    // Mapa posição → dado
    const byPos = {};
    state.expedicao.forEach((e) => {
      byPos[e.posicao] = e;
    });

    for (let pos = 1; pos <= EXPEDICAO_TOTAL; pos++) {
      let cell = document.getElementById(`bloco-exp-${pos}`);

      if (!cell) {
        cell = _criarCelula(pos, "expedicao");
        expedicaoGrid.appendChild(cell);
      }

      const dado = byPos[pos];
      // pedidoResponseDTO é o nome correto do campo no DTO do servidor
      const pedido = dado?.pedidoResponseDTO ?? null;

      _atualizarCelulaExpedicao(cell, pos, pedido);
    }

    _renderStatsExpedicao();
  }

  function _atualizarCelulaExpedicao(cell, pos, pedido) {
    // Remove classes de cor anteriores
    cell.classList.remove("bloco--vazio", "bloco--ocupado", "skeleton");

    if (pedido) {
      // Posição ocupada — exibe a OP do pedido
      cell.classList.add("bloco--ocupado");
      cell.innerHTML = `
        <span class="bloco__pos">${pos}</span>
        <span class="exp-bloco__op">OP ${_formatOP(pedido.ordemProducao)}</span>
      `;
    } else {
      // Posição livre
      cell.classList.add("bloco--vazio");
      cell.innerHTML = `<span class="bloco__pos">${pos}</span>`;
    }
  }

  /* ================================================================
     ESTATÍSTICAS
     ================================================================ */

  function _renderStatsEstoque() {
    // Conta por nome de cor (converte int → nome antes)
    const counts = { PRETO: 0, VERMELHO: 0, AZUL: 0, VAZIO: 0 };

    state.estoque.forEach((e) => {
      const nome = COR_INT_TO_NAME[e.corBloco] ?? "VAZIO";
      counts[nome] = (counts[nome] || 0) + 1;
    });

    if (statPreto) statPreto.textContent = counts.PRETO;
    if (statVermelho) statVermelho.textContent = counts.VERMELHO;
    if (statAzul) statAzul.textContent = counts.AZUL;
    if (statVazio) statVazio.textContent = counts.VAZIO;

    // Barra stacked — cada segmento tem largura proporcional
    const pct = (n) => `${((n / ESTOQUE_TOTAL) * 100).toFixed(1)}%`;
    if (fillPreto) fillPreto.style.width = pct(counts.PRETO);
    if (fillVermelho) fillVermelho.style.width = pct(counts.VERMELHO);
    if (fillAzul) fillAzul.style.width = pct(counts.AZUL);
    if (fillVazio) fillVazio.style.width = pct(counts.VAZIO);
  }

  function _renderStatsExpedicao() {
    const ocupados = state.expedicao.filter(
      (e) => e.pedidoResponseDTO !== null,
    ).length;

    if (expOcupado) expOcupado.textContent = ocupados;
    if (expTotal) expTotal.textContent = EXPEDICAO_TOTAL;
    if (expFill) {
      expFill.style.width = `${((ocupados / EXPEDICAO_TOTAL) * 100).toFixed(1)}%`;
    }
  }

  /* ================================================================
     SELEÇÃO DE BLOCOS (estoque)
     ================================================================ */

  function _toggleSelecao(pos) {
    if (state.selectedPos.has(pos)) {
      state.selectedPos.delete(pos);
    } else {
      state.selectedPos.add(pos);
    }

    // Atualiza visual da célula
    const cell = document.getElementById(`bloco-est-${pos}`);
    if (cell) {
      cell.classList.toggle("bloco--selected", state.selectedPos.has(pos));
    }

    _syncSelecaoUI();
  }

  function _limparSelecao() {
    state.selectedPos.forEach((pos) => {
      document
        .getElementById(`bloco-est-${pos}`)
        ?.classList.remove("bloco--selected");
    });
    state.selectedPos.clear();
    _syncSelecaoUI();
  }

  function _syncSelecaoUI() {
    const n = state.selectedPos.size;

    if (selectionInfo) {
      selectionInfo.textContent =
        n === 0
          ? "Nenhum selecionado"
          : `${n} bloco${n > 1 ? "s" : ""} selecionado${n > 1 ? "s" : ""}`;
    }

    // O botão só fica ativo com pelo menos 1 bloco selecionado E uma cor escolhida
    if (applyBtn) {
      applyBtn.disabled = n === 0 || state.activeColor === null;
    }
  }

  /* ================================================================
     PALETA DE CORES
     ================================================================ */

  colorBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      const cor = btn.dataset.color; // 'PRETO' | 'VERMELHO' | 'AZUL' | 'VAZIO'

      // Toggle: clica duas vezes na mesma cor desmarca
      state.activeColor = state.activeColor === cor ? null : cor;

      // Atualiza visual dos botões de cor
      colorBtns.forEach((b) => {
        b.classList.toggle(
          "color-btn--active",
          b.dataset.color === state.activeColor,
        );
      });

      _syncSelecaoUI();
    });
  });

  /* ================================================================
     APLICAR COR (mutação na API)
     ================================================================ */

  if (applyBtn) {
    applyBtn.addEventListener("click", async () => {
      if (!state.activeColor || state.selectedPos.size === 0) return;

      state.isApplying = true;
      applyBtn.disabled = true;

      const textoOriginal = applyBtn.textContent;
      applyBtn.textContent = "…";

      const posicoes = [...state.selectedPos];
      const erros = [];

      // Dispara uma requisição por posição selecionada
      for (const pos of posicoes) {
        try {
          if (state.activeColor === "VAZIO") {
            // Remove o bloco daquela posição
            await Api.put(`/api/estoque/remover/${pos}`);
          } else {
            // Adiciona/substitui a cor naquela posição
            await Api.put("/api/estoque/adicionar", {
              posicao: pos,
              corBloco: state.activeColor, // API espera o nome do enum como string
            });
          }
        } catch (err) {
          erros.push(`Pos. ${pos}: ${err.message}`);
        }
      }

      // Feedback ao usuário
      if (erros.length === 0) {
        Toast.success(
          `Cor aplicada em ${posicoes.length} bloco${posicoes.length > 1 ? "s" : ""}`,
        );
      } else {
        Toast.error(`${erros.length} erro(s). ${erros[0]}`);
      }

      // Refaz o fetch para garantir consistência com o banco
      await _fetchTudo();

      _limparSelecao();
      applyBtn.textContent = textoOriginal;
      state.isApplying = false;
    });
  }

  /* ================================================================
     ATALHOS DE TECLADO
     ================================================================ */

  document.addEventListener("keydown", (e) => {
    // Escape desmarca tudo
    if (e.key === "Escape") {
      _limparSelecao();
      // Desmarca a cor ativa também
      state.activeColor = null;
      colorBtns.forEach((b) => b.classList.remove("color-btn--active"));
      _syncSelecaoUI();
    }
  });

  /* ================================================================
     HELPERS PRIVADOS
     ================================================================ */

  /**
   * Cria uma célula de bloco e registra os listeners corretos.
   * @param {number} pos  - número da posição (1-based)
   * @param {'estoque'|'expedicao'} tipo
   */
  function _criarCelula(pos, tipo) {
    const div = document.createElement("div");

    if (tipo === "estoque") {
      div.id = `bloco-est-${pos}`;
      div.className = "bloco bloco--estoque bloco--vazio";
      div.addEventListener("click", () => _toggleSelecao(pos));
    } else {
      div.id = `bloco-exp-${pos}`;
      div.className = "bloco bloco--vazio";
    }

    return div;
  }

  /**
   * Formata o número de ordem de produção com zero à esquerda.
   * @param {number|null} op
   */
  function _formatOP(op) {
    if (op == null) return "—";
    return String(op).padStart(3, "0");
  }
});
