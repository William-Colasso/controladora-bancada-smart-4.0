

const POLL_INTERVAL_MS = 5_000;

const COR_TAMPA_HEX = Object.freeze({
  PRETO: '#333',
  VERMELHO: '#fc0518',
  AZUL: '#2b92d5',
});

const POSICAO_LABEL = Object.freeze({
  ESQUERDA: '← Esq',
  FRENTE: '↑ Frente',
  DIREITA: '→ Dir',
});

/* ------------------------------------------------------------------ */
/*  Estado                                                              */
/* ------------------------------------------------------------------ */

const state = {
  pedidos:      /** @type {PedidoDTO[]} */ ([]),
  filtered:     /** @type {PedidoDTO[]} */ ([]),
  activeFilter: 'TODOS',
  selectedId:   /** @type {number|null} */ (null),
  /** Snapshot do último render, indexado por id. @type {Map<number, PedidoDTO>} */
  snapshot: new Map(),
};

/* ------------------------------------------------------------------ */
/*  Helpers puros                                                       */
/* ------------------------------------------------------------------ */

/** @param {string} cor @returns {string} */
const tampaHex = cor => COR_TAMPA_HEX[cor] ?? '#555';

/** @param {string} pos @returns {string} */
const posicaoLabel = pos => POSICAO_LABEL[pos] ?? pos;

/** @param {number} n @returns {string} */
const countLabel = n => `${n} pedido${n !== 1 ? 's' : ''}`;

/**
 * Serializa os blocos para comparação rápida de igualdade.
 * @param {BlocoDTO[]} blocos
 * @returns {string}
 */
const blocosSig = blocos => JSON.stringify(blocos);

/* ------------------------------------------------------------------ */
/*  Builders de HTML — usados apenas no primeiro render                */
/* ------------------------------------------------------------------ */

/** @param {LaminaDTO} l @returns {string} */
function buildLaminaHTML(l) {
  return `
    <div class="lamina-row">
      <div class="lamina-swatch lamina-swatch--${l.cor}"></div>
      <span class="lamina-cor">${l.cor}</span>
      <span class="lamina-padrao">${l.padrao !== 'NENHUM' ? l.padrao : ''}</span>
      <span class="lamina-pos">${posicaoLabel(l.posicaoNoBloco)}</span>
    </div>`;
}

/** @param {BlocoDTO} b @param {number} i @returns {string} */
function buildBlocoDetailHTML(b, i) {
  const laminas = b.laminas ?? [];
  const posEl = b.estoque ? `<span class="bloco-detail-card__pos">Pos. ${b.estoque.posicao}</span>` : '';
  const laminasH = laminas.length > 0 ? laminas.map(buildLaminaHTML).join('') : '<p class="no-laminas">Sem lâminas</p>';

  return `
    <div class="bloco-detail-card" data-bloco-idx="${i}">
      <div class="bloco-detail-card__header">
        <div class="bloco-color-bar bloco-color-bar--${Utils.corBlocoClass(b.cor)}"></div>
        <span class="bloco-detail-card__name">Bloco ${i + 1} — ${Utils.corBlocoLabel(b.cor)}</span>
        ${posEl}
      </div>
      <div class="laminas-list">${laminasH}</div>
    </div>`;
}

/** @param {string} key @param {string} val @param {string} [field] @param {string} [style] */
function buildInfoItemHTML(key, val, field = '', style = '') {
  const fieldAttr = field ? ` data-field="${field}"` : '';
  const styleAttr = style ? ` style="${style}"` : '';
  return `
    <div class="info-item">
      <div class="info-item__key">${key}</div>
      <div class="info-item__val"${fieldAttr}${styleAttr}>${val}</div>
    </div>`;
}

/** @param {PedidoDTO} p @returns {string} — HTML completo de uma linha <tr> */
function buildFullRowHTML(p) {
  const blocos = p.blocos ?? [];
  return `
    <td data-cell="id">#${Utils.formatCount(p.id)}</td>
    <td data-cell="op">${p.ordemProducao ?? '—'}</td>
    <td data-cell="status">
      <span class="badge ${Utils.statusBadgeClass(p.status)}">${p.status}</span>
    </td>
    <td data-cell="tipo">
      <span class="tipo-chip ${Utils.tipoChipClass(p.tipoPedido)}">${p.tipoPedido}</span>
    </td>
    <td data-cell="tampa">
      <div class="tampa-visual">
        <div class="tampa-swatch" style="background:${tampaHex(p.corTampa)}"></div>
        ${p.corTampa}
      </div>
    </td>
    <td data-cell="blocos">
      <div class="blocos-preview">
        ${blocos.map(b => `<div class="mini-bloco mini-bloco--${Utils.corBlocoClass(b.cor)}"></div>`).join('')}
      </div>
    </td>
    <td data-cell="criacao">${Utils.formatDateTime(p.dataCriacao)}</td>
    <td data-cell="expedicao">${Utils.formatDateTime(p.dataEntradaExpedicao)}</td>
    <td><button class="pedido-start-button" data-id="${p.id}"></button></td>`;
}

/** @param {PedidoDTO} p @returns {string} — HTML completo do painel de detalhe */
function buildFullDetailHTML(p) {
  const blocos = p.blocos ?? [];
  return `
    <div class="info-grid">
      ${buildInfoItemHTML('Pedido', `#${Utils.formatCount(p.id)}`)}
      ${buildInfoItemHTML('Ordem Produção', p.ordemProducao ?? '—', 'op')}
      ${buildInfoItemHTML('Status',
    `<span class="badge ${Utils.statusBadgeClass(p.status)}">${p.status}</span>`, 'status')}
      ${buildInfoItemHTML('Tipo',
      `<span class="tipo-chip ${Utils.tipoChipClass(p.tipoPedido)}">${p.tipoPedido}</span>`, 'tipo')}
      ${buildInfoItemHTML('Cor da Tampa',
        `<div class="tampa-visual">
             <div class="tampa-swatch" style="background:${tampaHex(p.corTampa)}"></div>
             ${p.corTampa}
           </div>`, 'tampa')}
      ${buildInfoItemHTML('Criado em', Utils.formatDateTime(p.dataCriacao), 'criacao', 'font-size:12px')}
      ${buildInfoItemHTML('Entrada Expedição', Utils.formatDateTime(p.dataEntradaExpedicao), 'expedicao', 'font-size:12px')}
      ${buildInfoItemHTML('Blocos', String(blocos.length), 'blocos-count')}
    </div>
    <div class="blocos-detail-title">BLOCOS & LÂMINAS</div>
    <div class="blocos-detail-list" data-field="blocos-list">
      ${blocos.length > 0
      ? blocos.map(buildBlocoDetailHTML).join('')
      : '<p class="no-laminas">Nenhum bloco neste pedido.</p>'}
    </div>`;
}

/* ------------------------------------------------------------------ */
/*  Patch cirúrgico — helpers de atualização de nós existentes         */
/* ------------------------------------------------------------------ */

/**
 * Atualiza o innerHTML de um nó apenas se o conteúdo mudou.
 * @param {Element|null} el
 * @param {string}       html
 */
function patchInner(el, html) {
  if (el && el.innerHTML !== html) el.innerHTML = html;
}

/**
 * Atualiza o textContent de um nó apenas se o texto mudou.
 * @param {Element|null} el
 * @param {string}       text
 */
function patchText(el, text) {
  if (el && el.textContent !== text) el.textContent = text;
}

/**
 * Compara pedido novo com snapshot e aplica apenas as células que mudaram.
 * @param {HTMLTableRowElement} row
 * @param {PedidoDTO}           next
 * @param {PedidoDTO}           prev
 */
function patchRow(row, next, prev) {
  const cell = name => row.querySelector(`[data-cell="${name}"]`);

  if (next.ordemProducao !== prev.ordemProducao) {
    patchText(cell('op'), next.ordemProducao ?? '—');
  }

  if (next.status !== prev.status) {
    const badge = cell('status')?.querySelector('.badge');
    if (badge) {
      badge.className = `badge ${Utils.statusBadgeClass(next.status)}`;
      badge.textContent = next.status;
    }
  }

  if (next.tipoPedido !== prev.tipoPedido) {
    const chip = cell('tipo')?.querySelector('.tipo-chip');
    if (chip) {
      chip.className = `tipo-chip ${Utils.tipoChipClass(next.tipoPedido)}`;
      chip.textContent = next.tipoPedido;
    }
  }

  if (next.corTampa !== prev.corTampa) {
    const visual = cell('tampa')?.querySelector('.tampa-visual');
    if (visual) {
      visual.querySelector('.tampa-swatch').style.background = tampaHex(next.corTampa);
      const textNode = [...visual.childNodes].find(n => n.nodeType === Node.TEXT_NODE);
      if (textNode) textNode.textContent = next.corTampa;
    }
  }

  if (blocosSig(next.blocos ?? []) !== blocosSig(prev.blocos ?? [])) {
    const preview = cell('blocos')?.querySelector('.blocos-preview');
    patchInner(preview, (next.blocos ?? [])
      .map(b => `<div class="mini-bloco mini-bloco--${Utils.corBlocoClass(b.cor)}"></div>`)
      .join(''));
  }

  if (next.dataCriacao !== prev.dataCriacao) {
    patchText(cell('criacao'), Utils.formatDateTime(next.dataCriacao));
  }

  if (next.dataEntradaExpedicao !== prev.dataEntradaExpedicao) {
    patchText(cell('expedicao'), Utils.formatDateTime(next.dataEntradaExpedicao));
  }
}

/**
 * Aplica patch cirúrgico no painel de detalhe aberto.
 * @param {Element}   detailBody
 * @param {PedidoDTO} next
 * @param {PedidoDTO} prev
 */
function patchDetailPanel(detailBody, next, prev) {
  const field = name => detailBody.querySelector(`[data-field="${name}"]`);

  if (next.ordemProducao !== prev.ordemProducao) {
    patchText(field('op'), next.ordemProducao ?? '—');
  }

  if (next.status !== prev.status) {
    const badge = field('status')?.querySelector('.badge');
    if (badge) {
      badge.className = `badge ${Utils.statusBadgeClass(next.status)}`;
      badge.textContent = next.status;
    }
  }

  if (next.tipoPedido !== prev.tipoPedido) {
    const chip = field('tipo')?.querySelector('.tipo-chip');
    if (chip) {
      chip.className = `tipo-chip ${Utils.tipoChipClass(next.tipoPedido)}`;
      chip.textContent = next.tipoPedido;
    }
  }

  if (next.corTampa !== prev.corTampa) {
    const container = field('tampa');
    if (container) {
      container.querySelector('.tampa-swatch').style.background = tampaHex(next.corTampa);
      const visual = container.querySelector('.tampa-visual');
      const textNode = [...visual.childNodes].find(n => n.nodeType === Node.TEXT_NODE);
      if (textNode) textNode.textContent = next.corTampa;
    }
  }

  if (next.dataCriacao !== prev.dataCriacao) {
    patchText(field('criacao'), Utils.formatDateTime(next.dataCriacao));
  }

  if (next.dataEntradaExpedicao !== prev.dataEntradaExpedicao) {
    patchText(field('expedicao'), Utils.formatDateTime(next.dataEntradaExpedicao));
  }

  if (blocosSig(next.blocos ?? []) !== blocosSig(prev.blocos ?? [])) {
    const blocos = next.blocos ?? [];
    patchText(field('blocos-count'), String(blocos.length));
    patchInner(field('blocos-list'),
      blocos.length > 0
        ? blocos.map(buildBlocoDetailHTML).join('')
        : '<p class="no-laminas">Nenhum bloco neste pedido.</p>');
  }
}

/* ------------------------------------------------------------------ */
/*  Gerenciamento de linhas da tabela                                  */
/* ------------------------------------------------------------------ */

/**
 * Remove linhas cujos ids não estão mais no conjunto filtrado.
 * @param {Element}      tableBody
 * @param {Set<number>}  activeIds
 */
function removeStaleRows(tableBody, activeIds) {
  tableBody.querySelectorAll('tr[data-pedido-id]').forEach(row => {
    if (!activeIds.has(Number(row.dataset.pedidoId))) row.remove();
  });
}

/**
 * Cria linha nova (primeiro render) ou aplica patch cirúrgico (renders seguintes).
 * @param {Element}   tableBody
 * @param {PedidoDTO} pedido
 */
function upsertRow(tableBody, pedido) {
  let row = tableBody.querySelector(`tr[data-pedido-id="${pedido.id}"]`);

  if (!row) {
    row = document.createElement('tr');
    row.dataset.pedidoId = String(pedido.id);
    row.innerHTML = buildFullRowHTML(pedido);
    tableBody.appendChild(row);
  } else {
    const prev = state.snapshot.get(pedido.id);
    if (prev) patchRow(row, pedido, prev);
    row.classList.toggle('row--selected', pedido.id === state.selectedId);
  }

  state.snapshot.set(pedido.id, pedido);
}

/**
 * Exibe ou remove o empty-state conforme necessário.
 * @param {Element} tableBody
 */
function syncEmptyState(tableBody) {
  const emptyEl = tableBody.querySelector('.empty-row');
  const hasResults = state.filtered.length > 0;

  if (hasResults) { emptyEl?.remove(); return; }

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

/* ------------------------------------------------------------------ */
/*  Inicialização                                                       */
/* ------------------------------------------------------------------ */

document.addEventListener('DOMContentLoaded', () => {

  /* -- Seletores DOM ------------------------------------------------- */
  const tableBody = document.getElementById('pedidosTableBody');
  const detailPanel = document.getElementById('detailPanel');
  const detailBody = document.getElementById('detailBody');
  const detailClose = document.getElementById('detailClose');
  const filterBtns = document.querySelectorAll('.filter-btn[data-filter]');
  const countDisplay = document.getElementById('pedidosCount');
  const loadingRow = document.getElementById('loadingRow');

  /* -- Filtro --------------------------------------------------------- */
  function applyFilter() {
    state.filtered = state.activeFilter === 'TODOS'
      ? [...state.pedidos]
      : state.pedidos.filter(p => p.status === state.activeFilter);

    if (countDisplay) countDisplay.textContent = countLabel(state.filtered.length);
  }

  /* -- Render --------------------------------------------------------- */
  function renderTable() {
    loadingRow?.remove();

    const activeIds = new Set(state.filtered.map(p => p.id));
    removeStaleRows(tableBody, activeIds);
    state.filtered.forEach(pedido => upsertRow(tableBody, pedido));
    syncEmptyState(tableBody);

    syncDetailPanel();
  }

  function syncDetailPanel() {
    if (state.selectedId === null || !detailBody) return;

    const next = state.pedidos.find(p => p.id === state.selectedId);
    if (!next) { closeDetail(); return; }

    // Painel ainda não montado (ex: acaba de abrir)
    if (!detailBody.hasChildNodes()) {
      detailBody.innerHTML = buildFullDetailHTML(next);
      return;
    }

    // Patch cirúrgico: compara com snapshot
    const prev = state.snapshot.get(state.selectedId);
    if (prev) patchDetailPanel(detailBody, next, prev);
  }

  /* -- Painel de detalhe --------------------------------------------- */
  function openDetail(id) {
    const pedido = state.pedidos.find(p => p.id === id);
    if (!pedido) return;

    state.selectedId = id;

    tableBody.querySelectorAll('tr[data-pedido-id]')
      .forEach(r => r.classList.toggle('row--selected', Number(r.dataset.pedidoId) === id));

    detailBody.innerHTML = buildFullDetailHTML(pedido);
    detailPanel?.style.setProperty('display', 'block');
    detailPanel?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  function closeDetail() {
    state.selectedId = null;
    if (detailBody) detailBody.innerHTML = '';
    detailPanel?.style.setProperty('display', 'none');
    tableBody.querySelectorAll('tr[data-pedido-id]')
      .forEach(r => r.classList.remove('row--selected'));
  }

  /* -- Fetch ---------------------------------------------------------- */
  async function fetchPedidos() {
    try {
      state.pedidos = (await Api.get('/api/pedidos')) ?? [];
      applyFilter();
      renderTable();
    } catch (err) {
      console.error('[Pedidos] fetch error:', err);
    }
  }

  /* -- Eventos (delegação na tabela) ---------------------------------- */
  tableBody.addEventListener('click', e => {
    const startBtn = e.target.closest('.pedido-start-button');
    if (startBtn) {
      const ID = startBtn.dataset.id;
      startBtn.classList.add('started');

      fetch("http://localhost:8088/api/pedidos/" + ID, { method: "POST" })
        .then(res => res.json())
        .then(data => alert(data))
        .catch(err => console.error('[Start] erro:', err));

      e.stopPropagation();
      return;
    }

    const row = e.target.closest('tr[data-pedido-id]');
    if (row) openDetail(Number(row.dataset.pedidoId));
  });

  filterBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      state.activeFilter = btn.dataset.filter;
      filterBtns.forEach(b => b.classList.toggle('filter-btn--active', b === btn));
      applyFilter();
      renderTable();
    });
  });

  detailClose?.addEventListener('click', closeDetail);






  /* -- Boot ----------------------------------------------------------- */
  fetchPedidos();
  setInterval(fetchPedidos, POLL_INTERVAL_MS);
});