/**
 * pedidos.js
 * Responsabilidade EXCLUSIVA do front-end:
 *   — Busca e renderiza a lista de pedidos via /api/pedidos
 *   — Filtra por status sem re-request
 *   — Abre painel de detalhes inline (blocos, tampa, lâminas)
 *   — Polling para manter lista atualizada
 *
 * O Thymeleaf renderiza apenas o esqueleto HTML;
 * todos os dados são preenchidos por este script.
 */

document.addEventListener('DOMContentLoaded', () => {

  /* ------------------------------------------------------------------ */
  /*  Constantes                                                          */
  /* ------------------------------------------------------------------ */
  const POLL_INTERVAL = 5000;

  /* ------------------------------------------------------------------ */
  /*  Estado                                                              */
  /* ------------------------------------------------------------------ */
  const state = {
    pedidos:        [],   // Array<PedidoDTO>
    filtered:       [],
    activeFilter:   'TODOS',
    selectedId:     null,
  };

  /* ------------------------------------------------------------------ */
  /*  Seletores DOM                                                       */
  /* ------------------------------------------------------------------ */
  const tableBody    = document.getElementById('pedidosTableBody');
  const detailPanel  = document.getElementById('detailPanel');
  const detailClose  = document.getElementById('detailClose');
  const filterBtns   = document.querySelectorAll('.filter-btn[data-filter]');
  const countDisplay = document.getElementById('pedidosCount');
  const loadingRow   = document.getElementById('loadingRow');

  /* ------------------------------------------------------------------ */
  /*  Inicialização                                                       */
  /* ------------------------------------------------------------------ */
  fetchPedidos();
  setInterval(fetchPedidos, POLL_INTERVAL);

  filterBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      state.activeFilter = btn.dataset.filter;
      filterBtns.forEach(b => b.classList.toggle('filter-btn--active', b === btn));
      applyFilter();
      renderTable();
    });
  });

  if (detailClose) detailClose.addEventListener('click', closeDetail);

  /* ------------------------------------------------------------------ */
  /*  Fetch                                                               */
  /* ------------------------------------------------------------------ */
  async function fetchPedidos() {
    try {
      const data = await Api.get('/api/pedidos');
      state.pedidos = data || [];
      applyFilter();
      renderTable();
    } catch (err) {
      console.error('[Pedidos] fetch error:', err);
    }
  }

  /* ------------------------------------------------------------------ */
  /*  Filtro                                                              */
  /* ------------------------------------------------------------------ */
  function applyFilter() {
    if (state.activeFilter === 'TODOS') {
      state.filtered = [...state.pedidos];
    } else {
      state.filtered = state.pedidos.filter(p => p.status === state.activeFilter);
    }
    if (countDisplay) {
      countDisplay.textContent = `${state.filtered.length} pedido${state.filtered.length !== 1 ? 's' : ''}`;
    }
  }

  /* ------------------------------------------------------------------ */
  /*  Render — Tabela                                                     */
  /* ------------------------------------------------------------------ */
  function renderTable() {
    if (loadingRow) loadingRow.remove();

    // Remove linhas existentes sem destruir o DOM todo
    // (mantém a linha de detalhe se existir)
    const existingRows = tableBody.querySelectorAll('tr[data-pedido-id]');
    const existingIds  = new Set([...existingRows].map(r => Number(r.dataset.pedidoId)));
    const newIds       = new Set(state.filtered.map(p => p.id));

    // Remove linhas que não existem mais no filtro
    existingRows.forEach(row => {
      if (!newIds.has(Number(row.dataset.pedidoId))) row.remove();
    });

    // Adiciona/atualiza linhas
    state.filtered.forEach((pedido, idx) => {
      let row = tableBody.querySelector(`tr[data-pedido-id="${pedido.id}"]`);
      if (!row) {
        row = document.createElement('tr');
        row.dataset.pedidoId = pedido.id;
        row.addEventListener('click', () => openDetail(pedido.id));
        tableBody.appendChild(row);
      }
      row.classList.toggle('row--selected', pedido.id === state.selectedId);
      row.innerHTML = buildRowHTML(pedido);
    });

    // Empty state
    const emptyEl = tableBody.querySelector('.empty-row');
    if (state.filtered.length === 0) {
      if (!emptyEl) {
        const tr = document.createElement('tr');
        tr.className = 'empty-row';
        tr.innerHTML = `<td colspan="8">
          <div class="empty-state">
            <div class="empty-state__icon">📦</div>
            <div class="empty-state__text">Nenhum pedido encontrado</div>
          </div>
        </td>`;
        tableBody.appendChild(tr);
      }
    } else {
      if (emptyEl) emptyEl.remove();
    }

    // Re-renderiza o painel de detalhe se estiver aberto
    if (state.selectedId !== null) {
      const pedido = state.pedidos.find(p => p.id === state.selectedId);
      if (pedido) renderDetailPanel(pedido);
      else closeDetail();
    }
  }

  function buildRowHTML(pedido) {
    const blocos = pedido.blocos || [];
    const blocosHtml = blocos.map(b =>
      `<div class="mini-bloco mini-bloco--${Utils.corBlocoClass(b.cor)}"></div>`
    ).join('');

    return `
      <td>#${Utils.formatCount(pedido.id)}</td>
      <td>${pedido.ordemProducao ?? '—'}</td>
      <td>
        <span class="badge ${Utils.statusBadgeClass(pedido.status)}">
          ${pedido.status}
        </span>
      </td>
      <td>
        <span class="tipo-chip ${Utils.tipoChipClass(pedido.tipoPedido)}">
          ${pedido.tipoPedido}
        </span>
      </td>
      <td>
        <div class="tampa-visual">
          <div class="tampa-swatch" style="background:${corTampaHex(pedido.corTampa)}"></div>
          ${pedido.corTampa}
        </div>
      </td>
      <td>
        <div class="blocos-preview">${blocosHtml}</div>
      </td>
      <td>${Utils.formatDateTime(pedido.dataCriacao)}</td>
      <td>${Utils.formatDateTime(pedido.dataEntradaExpedicao)}</td>
    `;
  }

  /* ------------------------------------------------------------------ */
  /*  Painel de detalhes                                                  */
  /* ------------------------------------------------------------------ */
  function openDetail(id) {
    state.selectedId = id;
    const pedido = state.pedidos.find(p => p.id === id);
    if (!pedido) return;

    // Atualiza seleção visual na tabela
    tableBody.querySelectorAll('tr[data-pedido-id]').forEach(r => {
      r.classList.toggle('row--selected', Number(r.dataset.pedidoId) === id);
    });

    renderDetailPanel(pedido);
    if (detailPanel) {
      detailPanel.style.display = 'block';
      detailPanel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }

  function closeDetail() {
    state.selectedId = null;
    if (detailPanel) detailPanel.style.display = 'none';
    tableBody.querySelectorAll('tr[data-pedido-id]').forEach(r => r.classList.remove('row--selected'));
  }

  function renderDetailPanel(pedido) {
    const body = document.getElementById('detailBody');
    if (!body) return;

    const blocos = pedido.blocos || [];

    body.innerHTML = `
      <!-- Info grid -->
      <div class="info-grid">
        <div class="info-item">
          <div class="info-item__key">Pedido</div>
          <div class="info-item__val">#${Utils.formatCount(pedido.id)}</div>
        </div>
        <div class="info-item">
          <div class="info-item__key">Ordem Produção</div>
          <div class="info-item__val">${pedido.ordemProducao ?? '—'}</div>
        </div>
        <div class="info-item">
          <div class="info-item__key">Status</div>
          <div class="info-item__val">
            <span class="badge ${Utils.statusBadgeClass(pedido.status)}">${pedido.status}</span>
          </div>
        </div>
        <div class="info-item">
          <div class="info-item__key">Tipo</div>
          <div class="info-item__val">
            <span class="tipo-chip ${Utils.tipoChipClass(pedido.tipoPedido)}">${pedido.tipoPedido}</span>
          </div>
        </div>
        <div class="info-item">
          <div class="info-item__key">Cor da Tampa</div>
          <div class="info-item__val">
            <div class="tampa-visual">
              <div class="tampa-swatch" style="background:${corTampaHex(pedido.corTampa)}"></div>
              ${pedido.corTampa}
            </div>
          </div>
        </div>
        <div class="info-item">
          <div class="info-item__key">Criado em</div>
          <div class="info-item__val" style="font-size:12px">${Utils.formatDateTime(pedido.dataCriacao)}</div>
        </div>
        <div class="info-item">
          <div class="info-item__key">Entrada Expedição</div>
          <div class="info-item__val" style="font-size:12px">${Utils.formatDateTime(pedido.dataEntradaExpedicao)}</div>
        </div>
        <div class="info-item">
          <div class="info-item__key">Blocos</div>
          <div class="info-item__val">${blocos.length}</div>
        </div>
      </div>

      <!-- Detalhes de cada bloco -->
      <div class="blocos-detail-title">BLOCOS & LÂMINAS</div>
      <div class="blocos-detail-list">
        ${blocos.length === 0
          ? '<p class="no-laminas">Nenhum bloco neste pedido.</p>'
          : blocos.map((b, i) => buildBlocoDetailHTML(b, i)).join('')
        }
      </div>
    `;
  }

  function buildBlocoDetailHTML(bloco, idx) {
    const corClass = Utils.corBlocoClass(bloco.cor);
    const laminas  = bloco.laminas || [];

    const laminasHtml = laminas.length === 0
      ? '<p class="no-laminas">Sem lâminas</p>'
      : laminas.map(l => `
          <div class="lamina-row">
            <div class="lamina-swatch lamina-swatch--${l.cor}"></div>
            <span class="lamina-cor">${l.cor}</span>
            <span class="lamina-padrao">${l.padrao !== 'NENHUM' ? l.padrao : ''}</span>
            <span class="lamina-pos">${posicaoLabel(l.posicaoNoBloco)}</span>
          </div>
        `).join('');

    return `
      <div class="bloco-detail-card">
        <div class="bloco-detail-card__header">
          <div class="bloco-color-bar bloco-color-bar--${corClass}"></div>
          <span class="bloco-detail-card__name">Bloco ${idx + 1} — ${Utils.corBlocoLabel(bloco.cor)}</span>
          ${bloco.estoque ? `<span class="bloco-detail-card__pos">Pos. ${bloco.estoque.posicao}</span>` : ''}
        </div>
        <div class="laminas-list">${laminasHtml}</div>
      </div>
    `;
  }

  /* ------------------------------------------------------------------ */
  /*  Helpers                                                             */
  /* ------------------------------------------------------------------ */
  function corTampaHex(cor) {
    const map = { PRETO: '#333', VERMELHO: '#fc0518', AZUL: '#2b92d5' };
    return map[cor] || '#555';
  }

  function posicaoLabel(pos) {
    const map = { ESQUERDA: '← Esq', FRENTE: '↑ Frente', DIREITA: '→ Dir' };
    return map[pos] || pos;
  }

});
