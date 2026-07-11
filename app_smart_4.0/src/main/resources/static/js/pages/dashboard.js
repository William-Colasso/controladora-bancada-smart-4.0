// Página DASHBOARD — monitor SOMENTE LEITURA de estoque e expedição (a edição vive em /magazine).
// Estoque: grid do banco + espelho do magazine no CLP (divergências). Expedição: clique numa
// posição abre o histórico de pedidos que passaram por ela (destaque no atual).
// Dados iniciais via SSR; ao vivo via SSE ('estoque'/'expedicao'/'estacao-all').
import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';
import { createSse } from '../core/sse.js';
import { COR_INT_TO_NAME } from '../core/enums.js';
import {
  createExpedicaoCell, renderEstoqueCell, renderExpedicaoCell,
  createClpEstoqueCell, renderClpEstoqueCell, renderClpExpedicaoCell,
} from '../components/blocoCell.js';
import { createPedidoViewer } from '../components/pedidoViewer.js';
import { buildDetailHTML } from '../components/pedidoDetail.js';
import { initCommBanner } from '../components/commBanner.js';
import { formatOP } from '../core/format.js';

const ESTOQUE_TOTAL = 28;
const EXPEDICAO_TOTAL = 12;

const state = {
  estoque: [],
  expedicao: [],
  estoqueClp: null,     // snapshot do magazine lido do CLP (array de 28 ints), null até o 1º evento
  expedicaoClp: null,   // magazine de expedição do CLP (array de 12 OPs), null até o 1º evento
  clpRecebido: false,
};

const estoqueGrid = document.getElementById('estoqueGrid');
const clpEstoqueGrid = document.getElementById('clpEstoqueGrid');
const clpEstoqueDivergencias = document.getElementById('clpEstoqueDivergencias');
const clpExpedicaoGrid = document.getElementById('clpExpedicaoGrid');
const clpExpedicaoDivergencias = document.getElementById('clpExpedicaoDivergencias');

const expedicaoGrid = document.getElementById('expedicaoGrid');


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

// ── Estoque (somente leitura) ────────────────────────────────────────────────
function renderEstoque() {
  const byPos = {};
  state.estoque.forEach((e) => { byPos[e.posicao] = e; });

  for (let pos = 1; pos <= ESTOQUE_TOTAL; pos++) {
    let cell = document.getElementById(`bloco-est-${pos}`);
    if (!cell) {
      cell = document.createElement('div');
      cell.id = `bloco-est-${pos}`;
      cell.className = 'bloco bloco--vazio';
      estoqueGrid.appendChild(cell);
    }
    renderEstoqueCell(cell, pos, byPos[pos]?.corBloco ?? 0, false);
  }
  renderStatsEstoque();
  renderEstoqueClp(); // divergência depende do banco também → re-render quando o banco muda
}

// Grid do CLP (somente leitura): cor lida do magazine (posicoesOcupadas) e destaque das posições
// que divergem do banco. Fonte = evento SSE estacao-all (on-change); enquanto não chega, fica skeleton.
function renderEstoqueClp() {
  if (!clpEstoqueGrid) return;

  if (!state.clpRecebido) {
    if (clpEstoqueDivergencias) clpEstoqueDivergencias.textContent = 'Sem comunicação com o CLP';
    return;
  }

  const bancoByPos = {};
  state.estoque.forEach((e) => { bancoByPos[e.posicao] = e; });

  let divergencias = 0;
  for (let pos = 1; pos <= ESTOQUE_TOTAL; pos++) {
    let cell = document.getElementById(`bloco-clp-estoque${pos}`);
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

  if (clpEstoqueDivergencias) {
    clpEstoqueDivergencias.textContent = divergencias === 0
      ? 'Sincronizado com o banco'
      : `${divergencias} divergência${divergencias > 1 ? 's' : ''} vs. banco`;
  }
}


// Grid do CLP de expedição (somente leitura): OP guardada em cada posição do magazine e destaque
// das que divergem do banco (OP do pedidoResponseDTO). Fonte = SSE estacao-all da estação EXPEDICAO.
function renderExpedicaoClp() {
  if (!clpExpedicaoGrid) return;

  if (!state.clpRecebido) {
    if (clpExpedicaoDivergencias) clpExpedicaoDivergencias.textContent = 'Sem comunicação com o CLP';
    return;
  }

  const bancoByPos = {};
  state.expedicao.forEach((e) => { bancoByPos[e.posicao] = e; });

  let divergencias = 0;
  for (let pos = 1; pos <= EXPEDICAO_TOTAL; pos++) {
    let cell = document.getElementById(`bloco-clp-expedicao${pos}`);
    if (!cell) {
      cell = createExpedicaoCell(pos);
      cell.id = `bloco-clp-expedicao${pos}`;
      clpExpedicaoGrid.appendChild(cell);
    }
    const opClp = state.expedicaoClp?.[pos - 1] ?? 0;
    const opBanco = bancoByPos[pos]?.pedidoResponseDTO?.ordemProducao ?? 0;
    const divergente = opClp !== opBanco;
    if (divergente) divergencias++;
    renderClpExpedicaoCell(cell, pos, opClp, divergente);
  }

  if (clpExpedicaoDivergencias) {
    clpExpedicaoDivergencias.textContent = divergencias === 0
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

// ── Expedição: clique → histórico da posição ─────────────────────────────────
function renderExpedicao() {
  const byPos = {};
  state.expedicao.forEach((e) => { byPos[e.posicao] = e; });

  for (let pos = 1; pos <= EXPEDICAO_TOTAL; pos++) {
    let cell = document.getElementById(`bloco-exp-${pos}`);
    if (!cell) {
      cell = createExpedicaoCell(pos);
      expedicaoGrid.appendChild(cell);
    }
    cell.dataset.pos = pos;
    cell.classList.add('bloco--historico'); // cursor/hover: posição clicável
    cell.title = `Ver histórico da posição ${pos}`;
    renderExpedicaoCell(cell, pos, byPos[pos]?.pedidoResponseDTO ?? null);
  }
  renderStatsExpedicao();
  renderExpedicaoClp(); // divergência depende do banco também → re-render quando o banco muda
}

function renderStatsExpedicao() {
  const ocupados = state.expedicao.filter((e) => e.pedidoResponseDTO !== null).length;
  if (expOcupado) expOcupado.textContent = ocupados;
  if (expTotal) expTotal.textContent = EXPEDICAO_TOTAL;
  if (expFill) expFill.style.width = `${((ocupados / EXPEDICAO_TOTAL) * 100).toFixed(1)}%`;
}

// ─── Modal de histórico (lista + detalhe 3D do pedido selecionado) ───────────
const modal = document.getElementById('historico-modal');
const histLista = document.getElementById('historico-lista');
const histTitulo = document.getElementById('historico-titulo');
let modalViewer = null;
const getModalViewer = () =>
  (modalViewer ??= createPedidoViewer(document.getElementById('historico-viewer')));

let historico = [];   // pedidos da posição aberta (mais recente primeiro)
let atualId = null;   // id do pedido atualmente guardado na posição (destaque)

function itemHistoricoHTML(p, selecionado) {
  const atual = p.id === atualId;
  return `
    <button type="button" class="hist-item${atual ? ' hist-item--atual' : ''}${selecionado ? ' hist-item--selected' : ''}"
            data-pedido-id="${p.id}">
      <span class="hist-item__op">OP ${formatOP(p.ordemProducao)}</span>
      <span class="hist-item__meta">#${p.id}</span>
      ${atual ? '<span class="hist-item__tag">NA POSIÇÃO</span>' : ''}
    </button>`;
}

function selecionarPedido(id) {
  const pedido = historico.find((p) => p.id === id);
  if (!pedido) return;
  histLista.querySelectorAll('.hist-item').forEach((el) => {
    el.classList.toggle('hist-item--selected', Number(el.dataset.pedidoId) === id);
  });
  document.getElementById('historico-info').innerHTML = buildDetailHTML(pedido);
  getModalViewer().update(pedido);
}

async function abrirHistorico(pos) {
  try {
    historico = (await Api.get(`/api/expedicao/${pos}/pedidos`)) ?? [];
    const slot = state.expedicao.find((e) => e.posicao === pos);
    atualId = slot?.pedidoResponseDTO?.id ?? null;

    histTitulo.textContent = `Expedição · Posição ${pos}`;

    if (historico.length === 0) {
      histLista.innerHTML = '<p class="hist-vazio">Nenhum pedido passou por esta posição ainda.</p>';
      document.getElementById('historico-info').innerHTML = '';
      getModalViewer().update(null);
    } else {
      // Destaque no atual: ele vem primeiro na seleção default (senão o mais recente).
      const inicial = historico.find((p) => p.id === atualId) ?? historico[0];
      histLista.innerHTML = historico.map((p) => itemHistoricoHTML(p, p.id === inicial.id)).join('');
      selecionarPedido(inicial.id);
    }
    modal.showModal();
  } catch (err) {
    Toast.error(err.message || 'Falha ao carregar o histórico');
  }
}

expedicaoGrid.addEventListener('click', (e) => {
  const cell = e.target.closest('.bloco--historico');
  if (!cell) return;
  abrirHistorico(Number(cell.dataset.pos));
});
histLista?.addEventListener('click', (e) => {
  const item = e.target.closest('.hist-item');
  if (item) selecionarPedido(Number(item.dataset.pedidoId));
});
document.getElementById('historicoClose')?.addEventListener('click', () => modal.close());
modal?.addEventListener('click', (e) => { if (e.target === modal) modal.close(); });

// ── Dados: SSR inicial + SSE ao vivo ─────────────────────────────────────────
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
  if (d.estacao === 'estoque') {
    state.estoqueClp = d.dados?.posicoesOcupadas ?? null;
    state.clpRecebido = true;
    renderEstoqueClp();
  } else if (d.estacao === 'expedicao') {
    // magazine de expedição do CLP: orderExpedicao = array de 12 OPs (0 = posição vazia)
    state.expedicaoClp = d.dados?.orderExpedicao ?? null;
    state.clpRecebido = true;
    renderExpedicaoClp();
  }
});

carregarDadosIniciais();
initCommBanner(sse); // sem heartbeat → banner "sem comunicação" + indicadores vermelhos
sse.connect();
