// Página MAGAZINE — controle manual dos magazines físicos (o dashboard é somente leitura).
// Estoque: seleção múltipla + aplicar cor (PUT /api/estoque/...). Expedição: limpar posição
// (DELETE /api/expedicao/{pos} → solta o pedido no banco e zera a OP no CLP).
// Dados iniciais via SSR (hidden inputs); ao vivo via SSE ('estoque'/'expedicao').
import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';
import { createSse } from '../core/sse.js';
import {
  createEstoqueCell, createExpedicaoCell, renderEstoqueCell, renderMagazineExpedicaoCell,
} from '../components/blocoCell.js';
import { initCommBanner } from '../components/commBanner.js';

const ESTOQUE_TOTAL = 28;
const EXPEDICAO_TOTAL = 12;

const state = {
  estoque: [],
  expedicao: [],
  selectedPos: new Set(),
  activeColor: null,
};

const estoqueGrid = document.getElementById('estoqueGrid');
const expedicaoGrid = document.getElementById('expedicaoGrid');
const applyBtn = document.getElementById('applyColorBtn');
const selectionInfo = document.getElementById('selectionInfo');
const colorBtns = document.querySelectorAll('.color-btn[data-color]');

// ── Seleção do estoque (clique + arraste, igual ao antigo dashboard) ─────────
let selecting = false;

function toggleSelecao(pos) {
  pos = Number(pos);
  if (state.selectedPos.has(pos)) state.selectedPos.delete(pos);
  else state.selectedPos.add(pos);
  document.getElementById(`bloco-est-${pos}`)?.classList.toggle('bloco--selected', state.selectedPos.has(pos));
  syncSelecaoUI();
}

function limparSelecao() {
  state.selectedPos.forEach((pos) => {
    document.getElementById(`bloco-est-${pos}`)?.classList.remove('bloco--selected');
  });
  state.selectedPos.clear();
  syncSelecaoUI();
}

function syncSelecaoUI() {
  const n = state.selectedPos.size;
  if (selectionInfo) {
    selectionInfo.textContent = n === 0
      ? 'Nenhum selecionado'
      : `${n} bloco${n > 1 ? 's' : ''} selecionado${n > 1 ? 's' : ''}`;
  }
  if (applyBtn) applyBtn.disabled = n === 0 || state.activeColor === null;
}

function renderEstoque() {
  const byPos = {};
  state.estoque.forEach((e) => { byPos[e.posicao] = e; });

  for (let pos = 1; pos <= ESTOQUE_TOTAL; pos++) {
    let cell = document.getElementById(`bloco-est-${pos}`);
    if (!cell) {
      cell = createEstoqueCell(pos, toggleSelecao);
      estoqueGrid.appendChild(cell);
    }
    cell.dataset.pos = pos;
    renderEstoqueCell(cell, pos, byPos[pos]?.corBloco ?? 0, state.selectedPos.has(pos));
  }
}

// Arraste = mousedown num bloco + mouseenter nos vizinhos (capture p/ pegar células recriadas).
estoqueGrid.addEventListener('mousedown', (e) => {
  const bloco = e.target.closest('.bloco--estoque');
  if (!bloco) return;
  selecting = true;
  e.preventDefault(); // sem drag de imagem/texto
});
estoqueGrid.addEventListener('mouseenter', (e) => {
  if (selecting && e.target.classList?.contains('bloco--estoque')) {
    toggleSelecao(e.target.dataset.pos);
  }
}, true);
document.addEventListener('mouseup', () => { selecting = false; });

// ── Expedição (limpar posição) ───────────────────────────────────────────────
async function limparPosicao(pos) {
  if (!window.confirm(`Liberar a posição ${pos} da expedição? Remove o pedido da posição e zera a OP no CLP.`)) {
    return;
  }
  try {
    await Api.delete(`/api/expedicao/${pos}`);
    Toast.success(`Posição ${pos} liberada`);
    await refreshExpedicao();
  } catch (err) {
    Toast.error(err.message || `Falha ao liberar a posição ${pos}`);
  }
}

function renderExpedicao() {
  const byPos = {};
  state.expedicao.forEach((e) => { byPos[e.posicao] = e; });

  for (let pos = 1; pos <= EXPEDICAO_TOTAL; pos++) {
    let cell = document.getElementById(`bloco-exp-${pos}`);
    if (!cell) {
      cell = createExpedicaoCell(pos);
      expedicaoGrid.appendChild(cell);
    }
    renderMagazineExpedicaoCell(cell, pos, byPos[pos]?.pedidoResponseDTO ?? null, limparPosicao);
  }
}

// ── Dados: SSR inicial + SSE ao vivo + refresh pontual pós-mutação ───────────
function carregarDadosIniciais() {
  try {
    const estoqueEl = document.getElementById('initialEstoque');
    const expedicaoEl = document.getElementById('initialExpedicao');
    if (estoqueEl?.value) { state.estoque = JSON.parse(estoqueEl.value) || []; renderEstoque(); }
    if (expedicaoEl?.value) { state.expedicao = JSON.parse(expedicaoEl.value) || []; renderExpedicao(); }
  } catch (e) {
    console.warn('[Magazine] Falha ao carregar dados iniciais SSR:', e);
  }
}

const sse = createSse();
sse.on('estoque', (d) => { state.estoque = d.posicoes || []; renderEstoque(); });
sse.on('expedicao', (d) => { state.expedicao = d.posicoes || []; renderExpedicao(); });

async function refreshEstoque() {
  try {
    state.estoque = (await Api.get('/api/estoque')) || [];
    renderEstoque();
  } catch (err) {
    console.error('[Magazine] refreshEstoque:', err);
  }
}

async function refreshExpedicao() {
  try {
    state.expedicao = (await Api.get('/api/expedicao')) || [];
    renderExpedicao();
  } catch (err) {
    console.error('[Magazine] refreshExpedicao:', err);
  }
}

// ── Toolbar de cor ───────────────────────────────────────────────────────────
colorBtns.forEach((btn) => {
  btn.addEventListener('click', () => {
    const cor = btn.dataset.color;
    state.activeColor = state.activeColor === cor ? null : cor;
    colorBtns.forEach((b) => b.classList.toggle('color-btn--active', b.dataset.color === state.activeColor));
    syncSelecaoUI();
  });
});

if (applyBtn) {
  applyBtn.addEventListener('click', async () => {
    if (!state.activeColor || state.selectedPos.size === 0) return;

    applyBtn.disabled = true;
    const textoOriginal = applyBtn.textContent;
    applyBtn.textContent = '…';

    const posicoes = [...state.selectedPos];
    const erros = [];
    for (const pos of posicoes) {
      try {
        // adicionar aceita cor 0 (VAZIO) → serve também para esvaziar sem erro em posição já vazia
        await Api.put('/api/estoque/adicionar', { posicao: pos, corBloco: Number(state.activeColor) });
      } catch (err) {
        erros.push(`Pos. ${pos}: ${err.message}`);
      }
    }

    if (erros.length === 0) {
      Toast.success(`Cor aplicada em ${posicoes.length} bloco${posicoes.length > 1 ? 's' : ''}`);
    } else {
      Toast.error(`${erros.length} erro(s). ${erros[0]}`);
    }

    await refreshEstoque();
    limparSelecao();
    applyBtn.textContent = textoOriginal;
  });
}

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    limparSelecao();
    state.activeColor = null;
    colorBtns.forEach((b) => b.classList.remove('color-btn--active'));
    syncSelecaoUI();
  }
});

carregarDadosIniciais();
initCommBanner(sse); // sem heartbeat → banner "sem comunicação" + indicadores vermelhos
sse.connect();
