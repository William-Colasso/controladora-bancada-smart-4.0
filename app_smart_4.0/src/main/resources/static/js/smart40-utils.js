/**
 * smart40-utils.js
 * Utilitários compartilhados entre todas as páginas do Smart 4.0
 * Responsabilidade: helpers de UI e wrappers da API REST
 */

/* ============================================================
   API CLIENT — wrapper sobre fetch para /api/*
   ============================================================ */
const Api = (() => {

  const BASE = '';  // mesmo origin

  async function request(method, path, body) {
    const opts = {
      method,
      headers: { 'Content-Type': 'application/json' },
    };
    if (body !== undefined) opts.body = JSON.stringify(body);

    const res = await fetch(BASE + path, opts);

    if (!res.ok) {
      let msg = `HTTP ${res.status}`;
      try {
        const err = await res.json();
        msg = err.message || msg;
      } catch (_) { /* ignora */ }
      throw new Error(msg);
    }

    // 204 No Content
    if (res.status === 204) return null;
    return res.json();
  }

  return {
    get:    (path)         => request('GET',    path),
    post:   (path, body)   => request('POST',   path, body),
    put:    (path, body)   => request('PUT',    path, body),
    delete: (path)         => request('DELETE', path),
  };
})();

/* ============================================================
   TOAST — notificações não-intrusivas
   ============================================================ */
const Toast = (() => {

  let container;

  function ensureContainer() {
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      document.body.appendChild(container);
    }
  }

  function show(message, type = 'info', duration = 3500) {
    ensureContainer();
    const icons = { success: '✓', error: '✕', info: 'ℹ' };

    const el = document.createElement('div');
    el.className = `toast toast--${type}`;
    el.innerHTML = `<span style="font-weight:700">${icons[type] || ''}</span> ${message}`;
    container.appendChild(el);

    setTimeout(() => {
      el.style.animation = 'toastIn 200ms ease reverse forwards';
      setTimeout(() => el.remove(), 200);
    }, duration);
  }

  return {
    success: (msg) => show(msg, 'success'),
    error:   (msg) => show(msg, 'error'),
    info:    (msg) => show(msg, 'info'),
  };
})();

/* ============================================================
   UTILS — helpers genéricos
   ============================================================ */
const Utils = {

  /** Mapeia enum CorBloco (servidor) → classe CSS local */
  corBlocoClass(cor) {
    const map = { PRETO: 'preto', VERMELHO: 'vermelho', AZUL: 'azul', VAZIO: 'vazio' };
    return map[cor] || 'vazio';
  },

  /** Mapeia enum CorBloco → label em pt-BR */
  corBlocoLabel(cor) {
    const map = { PRETO: 'Preto', VERMELHO: 'Vermelho', AZUL: 'Azul', VAZIO: 'Vazio' };
    return map[cor] || cor;
  },

  /** Mapeia StatusPedido → badge class */
  statusBadgeClass(status) {
    const map = { PENDENTE: 'badge--yellow', PRODUCAO: 'badge--blue', CONCLUIDO: 'badge--green' };
    return map[status] || 'badge--dim';
  },

  /** Mapeia TipoPedido → chip class */
  tipoChipClass(tipo) {
    const map = { SIMPLES: 'tipo-chip--simples', DUPLO: 'tipo-chip--duplo', TRIPLO: 'tipo-chip--triplo' };
    return map[tipo] || '';
  },

  /** Formata ISO datetime → dd/mm/yyyy HH:mm */
  formatDateTime(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
  },

  /** Formata número de pedidos */
  formatCount(n) {
    return String(n).padStart(3, '0');
  },

  /** Debounce simples */
  debounce(fn, ms) {
    let t;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...args), ms);
    };
  },
};
