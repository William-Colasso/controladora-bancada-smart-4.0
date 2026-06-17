import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';
import { createPoller } from '../core/poller.js';
import { COR_INT_TO_NAME } from '../core/enums.js';
import {
  createEstoqueCell, createExpedicaoCell, renderEstoqueCell, renderExpedicaoCell,
} from '../components/blocoCell.js';

const POLL_INTERVAL = 3000;
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

const stats = {
  PRETO: document.getElementById('statPreto'),
  VERMELHO: document.getElementById('statVermelho'),
  AZUL: document.getElementById('statAzul'),
  VAZIO: document.getElementById('statVazio'),
};
const fills = {
  PRETO: document.querySelector('.fill-preto'),
  VERMELHO: document.querySelector('.fill-vermelho'),
  AZUL: document.querySelector('.fill-azul'),
  VAZIO: document.querySelector('.fill-vazio'),
};
const expOcupado = document.getElementById('expOcupado');
const expTotal = document.getElementById('expTotal');
const expFill = document.getElementById('expFill');

function toggleSelecao(pos) {
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
    } else if (!cell.dataset.listenerRegistrado) {
      cell.addEventListener('click', () => toggleSelecao(pos));
      cell.dataset.listenerRegistrado = 'true';
    }
    renderEstoqueCell(cell, pos, byPos[pos]?.corBloco ?? 0, state.selectedPos.has(pos));
  }
  renderStatsEstoque();
}

function renderStatsEstoque() {
  const counts = { PRETO: 0, VERMELHO: 0, AZUL: 0, VAZIO: 0 };
  state.estoque.forEach((e) => {
    const nome = COR_INT_TO_NAME[e.corBloco] ?? 'VAZIO';
    counts[nome] = (counts[nome] || 0) + 1;
  });

  const pct = (n) => `${((n / ESTOQUE_TOTAL) * 100).toFixed(1)}%`;
  Object.keys(counts).forEach((nome) => {
    if (stats[nome]) stats[nome].textContent = counts[nome];
    if (fills[nome]) fills[nome].style.width = pct(counts[nome]);
  });
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
    renderExpedicaoCell(cell, pos, byPos[pos]?.pedidoResponseDTO ?? null);
  }
  renderStatsExpedicao();
}

function renderStatsExpedicao() {
  const ocupados = state.expedicao.filter((e) => e.pedidoResponseDTO !== null).length;
  if (expOcupado) expOcupado.textContent = ocupados;
  if (expTotal) expTotal.textContent = EXPEDICAO_TOTAL;
  if (expFill) expFill.style.width = `${((ocupados / EXPEDICAO_TOTAL) * 100).toFixed(1)}%`;
}

function carregarDadosIniciais() {
  try {
    const estoqueEl = document.getElementById('initialEstoque');
    const expedicaoEl = document.getElementById('initialExpedicao');
    if (estoqueEl?.value) { state.estoque = JSON.parse(estoqueEl.value) || []; renderEstoque(); }
    if (expedicaoEl?.value) { state.expedicao = JSON.parse(expedicaoEl.value) || []; renderExpedicao(); }
  } catch (e) {
    console.warn('[Dashboard] Falha ao carregar dados iniciais SSR:', e);
  }
}

async function fetchTudo() {
  const [estoque, expedicao] = await Promise.all([
    Api.get('/api/estoque'),
    Api.get('/api/expedicao'),
  ]);
  return { estoque: estoque || [], expedicao: expedicao || [] };
}

const poller = createPoller(fetchTudo, POLL_INTERVAL, ({ estoque, expedicao }) => {
  state.estoque = estoque;
  state.expedicao = expedicao;
  renderEstoque();
  renderExpedicao();
});

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

    poller.pause();
    applyBtn.disabled = true;
    const textoOriginal = applyBtn.textContent;
    applyBtn.textContent = '…';

    const posicoes = [...state.selectedPos];
    const erros = [];
    for (const pos of posicoes) {
      try {
        if (state.activeColor === 'VAZIO') {
          await Api.put(`/api/estoque/remover/${pos}`);
        } else {
          await Api.put('/api/estoque/adicionar', { posicao: pos, corBloco: state.activeColor });
        }
      } catch (err) {
        erros.push(`Pos. ${pos}: ${err.message}`);
      }
    }

    if (erros.length === 0) {
      Toast.success(`Cor aplicada em ${posicoes.length} bloco${posicoes.length > 1 ? 's' : ''}`);
    } else {
      Toast.error(`${erros.length} erro(s). ${erros[0]}`);
    }

    await poller.refresh();
    limparSelecao();
    applyBtn.textContent = textoOriginal;
    poller.resume();
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
poller.start();
