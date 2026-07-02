import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';
import { createSse } from '../core/sse.js';
import { COR_INT_TO_NAME } from '../core/enums.js';
import {
  createEstoqueCell, createExpedicaoCell, renderEstoqueCell, renderExpedicaoCell,
  createClpEstoqueCell, renderClpEstoqueCell,
} from '../components/blocoCell.js';

const ESTOQUE_TOTAL = 28;
const EXPEDICAO_TOTAL = 12;

const state = {
  estoque: [],
  expedicao: [],
  estoqueClp: null,     // snapshot do magazine lido do CLP (array de 28 ints), null até o 1º evento
  clpRecebido: false,
  selectedPos: new Set(),
  activeColor: null,
};

const estoqueGrid = document.getElementById('estoqueGrid');
const clpEstoqueGrid = document.getElementById('clpEstoqueGrid');
const clpDivergencias = document.getElementById('clpDivergencias');
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



var selecting = false;
function renderEstoque() {
  const byPos = {};
  state.estoque.forEach((e) => { byPos[e.posicao] = e; });

  if (!estoqueGrid.dataset.listenerRegistrado) {
    estoqueGrid.addEventListener("mousedown", (e) => {
      selecting = true;
      ifBlocoToggle(e);
    });

    document.addEventListener("mouseup", () => {
      selecting = false;
    });

    estoqueGrid.addEventListener("mouseenter", (e) => {
      if (e.target.classList.contains("bloco--estoque") && selecting) {
        toggleSelecao(e.target.dataset.pos);
      }
    }, true); // capture: true — ver nota abaixo

    function ifBlocoToggle(e) {
      if (e.target.classList.contains("bloco--estoque") && selecting) {
        toggleSelecao(e.target.dataset.pos);
      }
    }

    estoqueGrid.dataset.listenerRegistrado = 'true';
  }
  for (let pos = 1; pos <= ESTOQUE_TOTAL; pos++) {
    let cell = document.getElementById(`bloco-est-${pos}`);
    if (!cell) {
      cell = createEstoqueCell(pos, toggleSelecao);
      estoqueGrid.appendChild(cell);
      cell.addEventListener("mouseenter", () => {
        if (selecting) {
          toggleSelecao(pos) // usa o pos do closure — mais direto
        }
      })
    } else {
      cell.dataset.pos = pos; // só garante que o dataset.pos está atualizado
    }


    renderEstoqueCell(cell, pos, byPos[pos]?.corBloco ?? 0, state.selectedPos.has(pos));

  }
  renderStatsEstoque();
  renderEstoqueClp(); // divergência depende do banco também → re-render quando o banco muda
}

// Grid do CLP (somente leitura): cor lida do magazine (posicoesOcupadas) e destaque das posições
// que divergem do banco. Fonte = evento SSE estacao-all (on-change); enquanto não chega, fica skeleton.
function renderEstoqueClp() {
  if (!clpEstoqueGrid) return;

  if (!state.clpRecebido) {
    if (clpDivergencias) clpDivergencias.textContent = 'Sem comunicação com o CLP';
    return;
  }

  const bancoByPos = {};
  state.estoque.forEach((e) => { bancoByPos[e.posicao] = e; });

  let divergencias = 0;
  for (let pos = 1; pos <= ESTOQUE_TOTAL; pos++) {
    let cell = document.getElementById(`bloco-clp-${pos}`);
    if (!cell) {
      cell = createClpEstoqueCell(pos);
      clpEstoqueGrid.appendChild(cell);
    }
    let corClp = state.estoqueClp?.[pos - 1] ?? 0;
    if (corClp < 0 || corClp > 3) corClp = 0; // valor inesperado do CLP → trata como vazio
    const corBanco = bancoByPos[pos]?.corBloco ?? 0;
    const divergente = corClp !== corBanco;
    if (divergente) divergencias++;
    renderClpEstoqueCell(cell, pos, corClp, divergente);
  }

  if (clpDivergencias) {
    clpDivergencias.textContent = divergencias === 0
      ? 'Sincronizado com o banco'
      : `${divergencias} divergência${divergencias > 1 ? 's' : ''} vs. banco`;
  }
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

// Tempo real via SSE (substitui o antigo polling de 3 s). O backend empurra o grid quando ele muda.
const sse = createSse();
sse.on('estoque', (d) => {
  state.estoque = d.posicoes || [];
  renderEstoque();
});
sse.on('expedicao', (d) => {
  state.expedicao = d.posicoes || [];
  renderExpedicao();
});
// Estoque do CLP: o bean *CLP completo chega por estação; filtramos a estação ESTOQUE e usamos
// o magazine (posicoesOcupadas) — array de 28 ints (índice c → posição c+1, valor = cor).
sse.on('estacao-all', (d) => {
  if (d.estacao !== 'estoque') return;
  state.estoqueClp = d.dados?.posicoesOcupadas ?? null;
  state.clpRecebido = true;
  renderEstoqueClp();
});

// Refresh pontual (one-shot, não é polling) para feedback imediato após uma mutação local,
// sem esperar o próximo ciclo do produtor SSE.
async function refreshEstoque() {
  try {
    state.estoque = (await Api.get('/api/estoque')) || [];
    renderEstoque();
  } catch (err) {
    console.error('[Dashboard] refreshEstoque:', err);
  }
}

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
sse.connect();
