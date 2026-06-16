import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';
import { createPoller } from '../core/poller.js';
import { buildRowHTML, patchRow } from '../components/pedidoRow.js';
import { buildDetailHTML, patchDetail } from '../components/pedidoDetail.js';

const POLL_INTERVAL_MS = 5000;
const countLabel = (n) => `${n} pedido${n !== 1 ? 's' : ''}`;

const state = {
  pedidos: [],
  filtered: [],
  activeFilter: 'TODOS',
  selectedId: null,
  snapshot: new Map(),
};

const tableBody = document.getElementById('pedidosTableBody');
const detailPanel = document.getElementById('detailPanel');
const detailBody = document.getElementById('detailBody');
const detailClose = document.getElementById('detailClose');
const filterBtns = document.querySelectorAll('.filter-btn[data-filter]');
const countDisplay = document.getElementById('pedidosCount');
const loadingRow = document.getElementById('loadingRow');

function applyFilter() {
  state.filtered = state.activeFilter === 'TODOS'
    ? [...state.pedidos]
    : state.pedidos.filter((p) => p.status === state.activeFilter);
  if (countDisplay) countDisplay.textContent = countLabel(state.filtered.length);
}

function removeStaleRows(activeIds) {
  tableBody.querySelectorAll('tr[data-pedido-id]').forEach((row) => {
    if (!activeIds.has(Number(row.dataset.pedidoId))) row.remove();
  });
}

function upsertRow(pedido) {
  let row = tableBody.querySelector(`tr[data-pedido-id="${pedido.id}"]`);
  if (!row) {
    row = document.createElement('tr');
    row.dataset.pedidoId = String(pedido.id);
    row.innerHTML = buildRowHTML(pedido);
    tableBody.appendChild(row);
  } else {
    const prev = state.snapshot.get(pedido.id);
    if (prev) patchRow(row, pedido, prev);
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

function syncDetailPanel() {
  if (state.selectedId === null || !detailBody) return;
  const next = state.pedidos.find((p) => p.id === state.selectedId);
  if (!next) { closeDetail(); return; }
  if (!detailBody.hasChildNodes()) {
    detailBody.innerHTML = buildDetailHTML(next);
    return;
  }
  const prev = state.snapshot.get(state.selectedId);
  if (prev) patchDetail(detailBody, next, prev);
}

function renderTable() {
  loadingRow?.remove();
  const activeIds = new Set(state.filtered.map((p) => p.id));
  removeStaleRows(activeIds);
  state.filtered.forEach(upsertRow);
  syncEmptyState();
  syncDetailPanel();
}

function openDetail(id) {
  const pedido = state.pedidos.find((p) => p.id === id);
  if (!pedido) return;
  state.selectedId = id;
  tableBody.querySelectorAll('tr[data-pedido-id]')
    .forEach((r) => r.classList.toggle('row--selected', Number(r.dataset.pedidoId) === id));
  detailBody.innerHTML = buildDetailHTML(pedido);
  detailPanel?.style.setProperty('display', 'block');
  detailPanel?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function closeDetail() {
  state.selectedId = null;
  if (detailBody) detailBody.innerHTML = '';
  detailPanel?.style.setProperty('display', 'none');
  tableBody.querySelectorAll('tr[data-pedido-id]').forEach((r) => r.classList.remove('row--selected'));
}

async function startPedido(id, btn) {
  btn.classList.add('started');
  try {
    const data = await Api.post(`/api/pedidos/${id}`);
    Toast.success(typeof data === 'string' ? data : 'Pedido enviado à produção.');
  } catch (err) {
    btn.classList.remove('started');
    Toast.error(err.message);
  }
}

tableBody.addEventListener('click', (e) => {
  const startBtn = e.target.closest('.pedido-start-button');
  if (startBtn) {
    e.stopPropagation();
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

const poller = createPoller(() => Api.get('/api/pedidos'), POLL_INTERVAL_MS, (pedidos) => {
  state.pedidos = pedidos ?? [];
  applyFilter();
  renderTable();
});

poller.start();
