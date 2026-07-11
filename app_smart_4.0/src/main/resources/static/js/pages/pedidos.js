import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';
import { createPoller } from '../core/poller.js';
import { buildRowHTML, patchRow } from '../components/pedidoRow.js';
import { buildDetailHTML, patchDetail } from '../components/pedidoDetail.js';
import { createPedidoViewer } from '../components/pedidoViewer.js';
import { renderFila } from '../components/filaPanel.js';
import { normalizeStatus } from '../core/enums.js';
import { formatDuracao } from '../core/format.js';

const POLL_INTERVAL_MS = 5000;
const countLabel = (n) => `${n} pedido${n !== 1 ? 's' : ''}`;

const state = {
  pedidos: [],
  filtered: [],
  fila: [],            // ids na ordem da fila de produção (head = em produção)
  filaSet: new Set(),  // lookup rápido "está na fila?" para o estado do botão
  activeFilter: 'TODOS',
  selectedId: null,
  snapshot: new Map(),
};

const tableBody = document.getElementById('pedidosTableBody');
const filaBody = document.getElementById('filaBody');
const detailPanel = document.getElementById('detailPanel');
const detailViewer = document.getElementById('detailViewer');
const detailInfo = document.getElementById('detailInfo');
const detailClose = document.getElementById('detailClose');
const detailEdit = document.getElementById('detailEdit');
const filterBtns = document.querySelectorAll('.filter-btn[data-filter]');
const countDisplay = document.getElementById('pedidosCount');
const loadingRow = document.getElementById('loadingRow');

// ─── Viewer — criado uma vez ao primeiro openDetail, reutilizado no polling ──
let viewer = null;
function getViewer() {
  if (!viewer && detailViewer) viewer = createPedidoViewer(detailViewer);
  return viewer;
}

// ─── Filtros e tabela ────────────────────────────────────────────────────────

function applyFilter() {
  state.filtered = state.activeFilter === 'TODOS'
    ? [...state.pedidos]
    : state.pedidos.filter((p) => normalizeStatus(p.status) === state.activeFilter);
  if (countDisplay) countDisplay.textContent = countLabel(state.filtered.length);
}

function removeStaleRows(activeIds) {
  tableBody.querySelectorAll('tr[data-pedido-id]').forEach((row) => {
    if (!activeIds.has(Number(row.dataset.pedidoId))) row.remove();
  });
}

function upsertRow(pedido) {
  const inQueue = state.filaSet.has(pedido.id);
  let row = tableBody.querySelector(`tr[data-pedido-id="${pedido.id}"]`);
  if (!row) {
    row = document.createElement('tr');
    row.dataset.pedidoId = String(pedido.id);
    row.innerHTML = buildRowHTML(pedido, inQueue);
    tableBody.appendChild(row);
  } else {
    const prev = state.snapshot.get(pedido.id);
    if (prev) patchRow(row, pedido, prev, inQueue);
    row.classList.toggle('row--selected', pedido.id === state.selectedId);
  }
  state.snapshot.set(pedido.id, pedido);
}

function syncEmptyState() {
  const emptyEl = tableBody.querySelector('.empty-row');
  if (state.filtered.length > 0) { emptyEl?.remove(); return; }
  if (!emptyEl) {
    const tr = document.createElement('tr');
    tr.className = 'empty-row';
    tr.innerHTML = `
      <td colspan="8">
        <div class="empty-state">
          <div class="empty-state__icon">📦</div>
          <div class="empty-state__text">Nenhum pedido encontrado</div>
        </div>
      </td>`;
    tableBody.appendChild(tr);
  }
}

// ─── Painel de detalhes ──────────────────────────────────────────────────────

// Botão Editar: só faz sentido enquanto o pedido pode ser alterado (PENDENTE).
function updateEditButton(pedido) {
  if (!detailEdit) return;
  const editavel = normalizeStatus(pedido.status) === 'PENDENTE';
  detailEdit.hidden = !editavel;
  if (editavel) detailEdit.href = `/formulario?id=${pedido.id}`;
}

function syncDetailPanel() {
  if (state.selectedId === null || !detailInfo) return;
  const next = state.pedidos.find((p) => p.id === state.selectedId);
  if (!next) { closeDetail(); return; }
  updateEditButton(next);
  if (!detailInfo.hasChildNodes()) {
    detailInfo.innerHTML = buildDetailHTML(next);
    getViewer()?.update(next);
    return;
  }
  const prev = state.snapshot.get(state.selectedId);
  if (prev) patchDetail(detailInfo, next, prev);
  getViewer()?.update(next);
}

function renderTable() {
  loadingRow?.remove();
  const activeIds = new Set(state.filtered.map((p) => p.id));
  removeStaleRows(activeIds);
  state.filtered.forEach(upsertRow);
  syncEmptyState();
  syncDetailPanel();
}

// Cruza os ids da fila com os pedidos carregados e renderiza a linha de produção.
function renderFilaPanel() {
  const byId = new Map(state.pedidos.map((p) => [p.id, p]));
  renderFila(filaBody, state.fila.map((id) => byId.get(id)));
}

function openDetail(id) {
  const pedido = state.pedidos.find((p) => p.id === id);
  if (!pedido) return;
  state.selectedId = id;
  tableBody.querySelectorAll('tr[data-pedido-id]')
    .forEach((r) => r.classList.toggle('row--selected', Number(r.dataset.pedidoId) === id));

  if (detailInfo) detailInfo.innerHTML = buildDetailHTML(pedido);
  updateEditButton(pedido);
  getViewer()?.update(pedido);

  detailPanel?.style.setProperty('display', 'block');
  detailPanel?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function closeDetail() {
  state.selectedId = null;
  if (detailEdit) detailEdit.hidden = true;
  if (detailInfo) detailInfo.innerHTML = '';
  viewer?.update(null);
  detailPanel?.style.setProperty('display', 'none');
  tableBody.querySelectorAll('tr[data-pedido-id]').forEach((r) => r.classList.remove('row--selected'));
}

async function startPedido(id, btn) {
  // Otimismo: entra na fila imediatamente (desabilita) — o próximo poll confirma o estado real.
  btn.dataset.state = 'queued';
  btn.disabled = true;
  try {
    await Api.post(`/api/pedidos/${id}`);
    Toast.success('Pedido enviado à fila de produção.');
    poller.refresh();
  } catch (err) {
    btn.dataset.state = 'idle';
    btn.disabled = false;
    Toast.error(err.message);
  }
}

// ─── Eventos ─────────────────────────────────────────────────────────────────

tableBody.addEventListener('click', (e) => {
  const startBtn = e.target.closest('.pedido-start-button');
  if (startBtn) {
    e.stopPropagation();
    if (startBtn.disabled) return; // já na fila / em produção / concluído
    startPedido(startBtn.dataset.id, startBtn);
    return;
  }
  const row = e.target.closest('tr[data-pedido-id]');
  if (row) openDetail(Number(row.dataset.pedidoId));
});

filterBtns.forEach((btn) => {
  btn.addEventListener('click', () => {
    state.activeFilter = btn.dataset.filter;
    filterBtns.forEach((b) => b.classList.toggle('filter-btn--active', b === btn));
    applyFilter();
    renderTable();
  });
});

detailClose?.addEventListener('click', closeDetail);

// ─── Polling ─────────────────────────────────────────────────────────────────

const poller = createPoller(
  () => Promise.all([Api.get('/api/pedidos'), Api.get('/api/pedidos/fila')]),
  POLL_INTERVAL_MS,
  ([pedidos, fila]) => {
    state.pedidos = pedidos ?? [];
    state.fila = fila ?? [];
    state.filaSet = new Set(state.fila);
    applyFilter();
    renderTable();
    renderFilaPanel();
  });

poller.start();

// Atualiza todos os cronômetros ativos na página a cada segundo.
setInterval(() => {
  document.querySelectorAll('[data-cronometro-start]').forEach((el) => {
    el.textContent = formatDuracao(el.dataset.cronometroStart);
  });
}, 1000);
