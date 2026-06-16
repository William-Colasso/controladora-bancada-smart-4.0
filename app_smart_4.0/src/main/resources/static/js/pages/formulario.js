import { Api } from '../core/api.js';
import { createLaminaRow, syncAddButton, emptyPlaceholder } from '../components/laminaRow.js';

const BLOCO_IDS = ['bloco-1', 'bloco-2', 'bloco-3'];

const opcoesLamina = {
  cores: window.SMART_ENUMS?.coresLaminas ?? [],
  padroes: window.SMART_ENUMS?.padroes ?? [],
  posicoes: window.SMART_ENUMS?.posicoes ?? [],
};

const tipoPedidoEl = document.getElementById('tipo-pedido');

function resetBloco(card) {
  card.querySelector('.laminas-list').innerHTML = emptyPlaceholder();
  syncAddButton(card.querySelector('.btn-add-lam'), 0);
}

function syncBlocos() {
  const qty = parseInt(tipoPedidoEl.value, 10);
  BLOCO_IDS.forEach((id, idx) => {
    const card = document.getElementById(id);
    const active = idx < qty;
    card.classList.toggle('inactive', !active);
    if (!active) resetBloco(card);
  });
}

function addLamina(btn) {
  const list = btn.closest('.bloco-body').querySelector('.laminas-list');
  if (list.querySelectorAll('.lamina-row').length >= 3) return;

  list.querySelector('.empty-lam')?.remove();

  const row = createLaminaRow(opcoesLamina, (target) => {
    target.remove();
    const remaining = list.querySelectorAll('.lamina-row').length;
    if (!remaining) list.innerHTML = emptyPlaceholder();
    syncAddButton(btn, remaining);
  });

  list.appendChild(row);
  syncAddButton(btn, list.querySelectorAll('.lamina-row').length);
}

function buildPayload() {
  const blocos = [];
  BLOCO_IDS.forEach((id) => {
    const card = document.getElementById(id);
    if (card.classList.contains('inactive')) return;

    const cor = parseInt(card.querySelector('.cor-bloco-sel').value, 10);
    const laminas = [...card.querySelectorAll('.lamina-row')].map((row) => ({
      cor: parseInt(row.querySelector('.lam-cor').value, 10),
      padrao: parseInt(row.querySelector('.lam-pad').value, 10),
      posicao: parseInt(row.querySelector('.lam-pos').value, 10),
    }));
    blocos.push({ cor, laminas });
  });

  return {
    tipoPedido: parseInt(tipoPedidoEl.value, 10),
    corTampa: parseInt(document.getElementById('cor-tampa').value, 10),
    blocos,
  };
}

function showResponse(ok, status, body) {
  const panel = document.getElementById('resp-panel');
  const titleEl = document.getElementById('resp-title');
  const bodyEl = document.getElementById('resp-body');

  panel.style.display = 'block';
  panel.className = ok ? 'ok' : 'err';
  titleEl.style.color = ok ? 'var(--color-green)' : 'var(--color-red)';
  titleEl.textContent = ok ? `✓  PEDIDO CRIADO  ·  HTTP ${status}` : `✕  ERRO  ·  HTTP ${status}`;
  bodyEl.textContent = typeof body === 'string' ? body : JSON.stringify(body, null, 2);
  panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

async function enviarPedido() {
  const btn = document.getElementById('btn-submit');
  btn.disabled = true;
  btn.classList.add('loading');
  btn.textContent = 'PROCESSANDO...';

  try {
    const data = await Api.post('/api/pedidos', buildPayload());
    showResponse(true, 201, data);
  } catch (err) {
    const panel = document.getElementById('resp-panel');
    const titleEl = document.getElementById('resp-title');
    panel.style.display = 'block';
    panel.className = 'err';
    titleEl.style.color = 'var(--color-red)';
    titleEl.textContent = '✕  ERRO';
    document.getElementById('resp-body').textContent = err.message;
    panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  } finally {
    btn.disabled = false;
    btn.classList.remove('loading');
    btn.textContent = 'ENVIAR PEDIDO';
  }
}

function limparForm() {
  tipoPedidoEl.selectedIndex = 0;
  document.getElementById('cor-tampa').selectedIndex = 0;

  BLOCO_IDS.forEach((id) => {
    const card = document.getElementById(id);
    card.querySelector('.cor-bloco-sel').selectedIndex = 0;
    resetBloco(card);
  });

  const panel = document.getElementById('resp-panel');
  panel.style.display = 'none';
  panel.className = '';
  syncBlocos();
}

tipoPedidoEl.addEventListener('change', syncBlocos);
document.getElementById('btn-submit').addEventListener('click', enviarPedido);
document.getElementById('btn-clear').addEventListener('click', limparForm);
document.querySelectorAll('.btn-add-lam').forEach((btn) => {
  btn.addEventListener('click', () => addLamina(btn));
});

syncBlocos();
