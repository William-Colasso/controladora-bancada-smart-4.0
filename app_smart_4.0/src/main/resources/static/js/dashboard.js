/**
 * dashboard.js
 * Responsabilidade EXCLUSIVA do front-end:
 *   — Renderiza os grids de blocos de estoque e expedição
 *   — Gerencia seleção de blocos e paleta de cores
 *   — Envia mutações para /api/estoque via Api.*
 *   — Faz polling a cada POLL_INTERVAL ms para manter os dados atualizados
 *
 * Responsabilidade do servidor (Thymeleaf):
 *   — Renderiza o HTML estrutural (grids vazios, toolbar)
 *   — Fornece os dados iniciais via atributos `data-*` para
 *     evitar um flash de "tudo vazio" no primeiro carregamento
 */

document.addEventListener('DOMContentLoaded', () => {

  /* ------------------------------------------------------------------ */
  /*  Constantes                                                          */
  /* ------------------------------------------------------------------ */
  const POLL_INTERVAL      = 3000;  // ms
  const ESTOQUE_TOTAL      = 28;
  const EXPEDICAO_TOTAL    = 12;

  /* ------------------------------------------------------------------ */
  /*  Estado local                                                        */
  /* ------------------------------------------------------------------ */
  const state = {
    estoque:         [],  // Array<{id, posicao, corBloco}>
    expedicao:       [],  // Array<{id, posicao, pedidoDTO}>
    selectedPos:     new Set(),  // posições do estoque selecionadas
    activeColor:     null,       // cor a aplicar ('PRETO'|'VERMELHO'|'AZUL'|'VAZIO')
    polling:         true,
    applyingColor:   false,
  };

  /* ------------------------------------------------------------------ */
  /*  Seletores DOM                                                       */
  /* ------------------------------------------------------------------ */
  const estoqueGrid   = document.getElementById('estoqueGrid');
  const expedicaoGrid = document.getElementById('expedicaoGrid');
  const applyBtn      = document.getElementById('applyColorBtn');
  const selectionInfo = document.getElementById('selectionInfo');
  const colorBtns     = document.querySelectorAll('.color-btn[data-color]');

  /* Estatísticas */
  const statPreto    = document.getElementById('statPreto');
  const statVermelho = document.getElementById('statVermelho');
  const statAzul     = document.getElementById('statAzul');
  const statVazio    = document.getElementById('statVazio');
  const ocupacaoFill = document.getElementById('ocupacaoFill');

  const expOcupado   = document.getElementById('expOcupado');
  const expTotal     = document.getElementById('expTotal');
  const expFill      = document.getElementById('expFill');

  /* ------------------------------------------------------------------ */
  /*  Inicialização                                                       */
  /* ------------------------------------------------------------------ */

  // Carrega dados iniciais que o Thymeleaf injetou (evita flash vazio)
  const initialEstoque   = JSON.parse(document.getElementById('initialEstoque')?.value   || '[]');
  const initialExpedicao = JSON.parse(document.getElementById('initialExpedicao')?.value || '[]');

  if (initialEstoque.length)   { state.estoque   = initialEstoque;   renderEstoque(); }
  if (initialExpedicao.length) { state.expedicao = initialExpedicao; renderExpedicao(); }

  // Fetch imediato + polling
  fetchAll();
  setInterval(() => { if (state.polling && !state.applyingColor) fetchAll(); }, POLL_INTERVAL);

  /* ------------------------------------------------------------------ */
  /*  Fetch de dados                                                      */
  /* ------------------------------------------------------------------ */
  async function fetchAll() {
    try {
      const [estoque, expedicao] = await Promise.all([
        Api.get('/api/estoque/todos'),
        Api.get('/api/expedicao'),
      ]);
      state.estoque   = estoque   || [];
      state.expedicao = expedicao || [];
      renderEstoque();
      renderExpedicao();
    } catch (err) {
      console.error('[Dashboard] fetchAll error:', err);
      // Não exibe toast no polling silencioso
    }
  }

  /* ------------------------------------------------------------------ */
  /*  Render — Estoque                                                    */
  /* ------------------------------------------------------------------ */
  function renderEstoque() {
    // Cria um mapa posição → dado para acesso O(1)
    const byPos = {};
    state.estoque.forEach(e => { byPos[e.posicao] = e; });

    // Atualiza as células existentes ou cria se necessário
    for (let pos = 1; pos <= ESTOQUE_TOTAL; pos++) {
      let cell = document.getElementById(`bloco-est-${pos}`);
      if (!cell) {
        cell = criarBlocoCell(pos, 'estoque');
        estoqueGrid.appendChild(cell);
      }
      const dado = byPos[pos];
      atualizarBlocoCell(cell, pos, dado ? dado.corBloco : 'VAZIO', 'estoque', dado);
    }

    atualizarStatsEstoque();
  }

  function criarBlocoCell(pos, tipo) {
    const div = document.createElement('div');
    div.id = `bloco-${tipo === 'estoque' ? 'est' : 'exp'}-${pos}`;
    div.className = `bloco bloco--${tipo === 'estoque' ? 'estoque' : 'expedicao'} bloco--vazio`;
    if (tipo === 'estoque') {
      div.addEventListener('click', () => toggleSelectBloco(pos));
    }
    return div;
  }

  function atualizarBlocoCell(cell, pos, cor, tipo, dado) {
    const corClass = Utils.corBlocoClass(cor);
    // Remove classes de cor anteriores
    cell.className = cell.className.replace(/bloco--(preto|vermelho|azul|vazio|ocupado)/g, '');
    cell.classList.add(`bloco--${corClass}`);

    // Mantém seleção visual
    if (tipo === 'estoque' && state.selectedPos.has(pos)) {
      cell.classList.add('bloco--selected');
    }

    let inner = `<span class="bloco__pos">${pos}</span>`;

    if (tipo === 'expedicao' && dado && dado.pedidoDTO) {
      cell.className = cell.className.replace(/bloco--vazio/g, '');
      cell.classList.add('bloco--ocupado');
      inner += `<span class="exp-bloco__op">OP ${dado.pedidoDTO.ordemProducao ?? '—'}</span>`;
    }

    cell.innerHTML = inner;
  }

  /* ------------------------------------------------------------------ */
  /*  Render — Expedição                                                  */
  /* ------------------------------------------------------------------ */
  function renderExpedicao() {
    // Cria mapa posição → dado
    const byPos = {};
    state.expedicao.forEach(e => { byPos[e.posicao] = e; });

    for (let pos = 1; pos <= EXPEDICAO_TOTAL; pos++) {
      let cell = document.getElementById(`bloco-exp-${pos}`);
      if (!cell) {
        cell = criarBlocoCell(pos, 'expedicao');
        expedicaoGrid.appendChild(cell);
      }
      const dado = byPos[pos];
      const cor  = dado && dado.pedidoDTO ? 'OCUPADO' : 'VAZIO';
      atualizarBlocoCell(cell, pos, cor, 'expedicao', dado);
    }

    atualizarStatsExpedicao();
  }

  /* ------------------------------------------------------------------ */
  /*  Estatísticas                                                        */
  /* ------------------------------------------------------------------ */
  function atualizarStatsEstoque() {
    const counts = { PRETO: 0, VERMELHO: 0, AZUL: 0, VAZIO: 0 };
    state.estoque.forEach(e => { counts[e.corBloco] = (counts[e.corBloco] || 0) + 1; });

    if (statPreto)    statPreto.textContent    = counts.PRETO;
    if (statVermelho) statVermelho.textContent = counts.VERMELHO;
    if (statAzul)     statAzul.textContent     = counts.AZUL;
    if (statVazio)    statVazio.textContent    = counts.VAZIO;

    const ocupados = ESTOQUE_TOTAL - (counts.VAZIO || 0);
    if (ocupacaoFill) ocupacaoFill.style.width = `${(ocupados / ESTOQUE_TOTAL) * 100}%`;
  }

  function atualizarStatsExpedicao() {
    const ocupados = state.expedicao.filter(e => e.pedidoDTO).length;
    if (expOcupado) expOcupado.textContent = ocupados;
    if (expTotal)   expTotal.textContent   = EXPEDICAO_TOTAL;
    if (expFill)    expFill.style.width    = `${(ocupados / EXPEDICAO_TOTAL) * 100}%`;
  }

  /* ------------------------------------------------------------------ */
  /*  Seleção de blocos no estoque                                        */
  /* ------------------------------------------------------------------ */
  function toggleSelectBloco(pos) {
    if (state.selectedPos.has(pos)) {
      state.selectedPos.delete(pos);
    } else {
      state.selectedPos.add(pos);
    }
    const cell = document.getElementById(`bloco-est-${pos}`);
    if (cell) cell.classList.toggle('bloco--selected', state.selectedPos.has(pos));
    atualizarSelectionUI();
  }

  function clearSelection() {
    state.selectedPos.forEach(pos => {
      const cell = document.getElementById(`bloco-est-${pos}`);
      if (cell) cell.classList.remove('bloco--selected');
    });
    state.selectedPos.clear();
    atualizarSelectionUI();
  }

  function atualizarSelectionUI() {
    const count = state.selectedPos.size;
    if (selectionInfo) {
      selectionInfo.textContent = count === 0
        ? 'Nenhum selecionado'
        : `${count} bloco${count > 1 ? 's' : ''} selecionado${count > 1 ? 's' : ''}`;
    }
    if (applyBtn) applyBtn.disabled = count === 0 || !state.activeColor;
  }

  /* ------------------------------------------------------------------ */
  /*  Paleta de cores                                                     */
  /* ------------------------------------------------------------------ */
  colorBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const cor = btn.dataset.color;  // 'PRETO' | 'VERMELHO' | 'AZUL' | 'VAZIO'
      state.activeColor = (state.activeColor === cor) ? null : cor;

      colorBtns.forEach(b => b.classList.toggle('color-btn--active', b.dataset.color === state.activeColor));
      if (applyBtn) applyBtn.disabled = state.selectedPos.size === 0 || !state.activeColor;
    });
  });

  /* ------------------------------------------------------------------ */
  /*  Aplicar cor nos blocos selecionados                                 */
  /* ------------------------------------------------------------------ */
  if (applyBtn) {
    applyBtn.addEventListener('click', async () => {
      if (!state.activeColor || state.selectedPos.size === 0) return;

      state.applyingColor = true;
      applyBtn.disabled = true;
      applyBtn.textContent = '...';

      const positions = [...state.selectedPos];
      const errors = [];

      for (const pos of positions) {
        try {
          if (state.activeColor === 'VAZIO') {
            await Api.put(`/api/estoque/remover/${pos}`);
          } else {
            await Api.put('/api/estoque/adicionar', {
              posicao: pos,
              corBloco: state.activeColor,
            });
          }
        } catch (err) {
          errors.push(`Pos ${pos}: ${err.message}`);
        }
      }

      if (errors.length) {
        Toast.error(`Erro em ${errors.length} posição(ões). ${errors[0]}`);
      } else {
        Toast.success(`Cor aplicada em ${positions.length} bloco${positions.length > 1 ? 's' : ''}`);
      }

      // Re-busca dados após mutação
      await fetchAll();
      clearSelection();
      applyBtn.textContent = 'Aplicar';
      state.applyingColor = false;
    });
  }

  /* ------------------------------------------------------------------ */
  /*  Atalhos de teclado                                                  */
  /* ------------------------------------------------------------------ */
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') clearSelection();
    // Ctrl+A = selecionar todos os blocos não vazios? (opcional, melhoria futura)
  });

});
