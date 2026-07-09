import { Api } from '../core/api.js';
import { createLaminaRow, syncAddButton, emptyPlaceholder } from '../components/laminaRow.js';
import { createPedidoViewer } from '../components/pedidoViewer.js';

const BLOCO_IDS = ['bloco-1', 'bloco-2', 'bloco-3'];

// Modo edição: window.PEDIDO_EDIT chega como string JSON (Jackson) ou null.
let PEDIDO_EDIT = window.PEDIDO_EDIT ?? null;
if (typeof PEDIDO_EDIT === 'string') {
  try { PEDIDO_EDIT = JSON.parse(PEDIDO_EDIT); } catch { PEDIDO_EDIT = null; }
}
const EDIT_ID = PEDIDO_EDIT?.id ?? null;

let TAMPA_HABILITADA = window.TAMPA_HABILITADA ?? null
if (typeof TAMPA_HABILITADA === 'string') {
  try { TAMPA_HABILITADA = JSON.parse(TAMPA_HABILITADA); } catch { TAMPA_HABILITADA = null }
}

const opcoesLamina = {
  cores: window.SMART_ENUMS?.coresLaminas ?? [],
  padroes: window.SMART_ENUMS?.padroes ?? [],
  posicoes: window.SMART_ENUMS?.posicoes ?? [],
};

const tipoPedidoEl = document.getElementById('tipo-pedido');
const ordemProducaoEl = document.getElementById('ordem-producao');
// OP sugerida pelo servidor (th:value) — usada para restaurar no "Limpar".
const OP_SUGERIDA = ordemProducaoEl?.value ?? '';

// ─── Preview 3D ──────────────────────────────────────────────────────────────

const viewerEl = document.getElementById('formulario-viewer');
const viewer = viewerEl ? createPedidoViewer(viewerEl) : null;

// Constrói um objeto compatível com o DTO de resposta a partir do payload atual
function buildPreviewPedido() {
  const p = buildPayload();
  if (!p.blocos.length) return null;
  return {
    corTampa: p.corTampa,
    blocos: p.blocos.map((b) => ({
      cor: b.cor,
      laminas: (b.laminas ?? []).map((l) => ({
        cor: l.cor,
        padrao: l.padrao,
        posicaoNoBloco: l.posicao, // payload usa 'posicao'; viewer usa 'posicaoNoBloco'
      })),
    })),
  };
}

function updatePreview() {
  viewer?.update(buildPreviewPedido());
}

// ─── Blocos / lâminas ────────────────────────────────────────────────────────

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
  updatePreview();
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
    updatePreview();
  });

  list.appendChild(row);
  syncAddButton(btn, list.querySelectorAll('.lamina-row').length);
  updatePreview();
}

// ─── Payload ─────────────────────────────────────────────────────────────────

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
    // OP vazia → null: o backend usa a próxima OP automática (MAX+1).
    ordemProducao: parseInt(ordemProducaoEl.value, 10) || null,
    blocos,
  };
}

// ─── Resposta ─────────────────────────────────────────────────────────────────

function showResponse(ok, status, body) {
  const panel = document.getElementById('resp-panel');
  const titleEl = document.getElementById('resp-title');
  const bodyEl = document.getElementById('resp-body');

  panel.style.display = 'block';
  panel.className = ok ? 'ok' : 'err';
  titleEl.style.color = ok ? 'var(--color-green)' : 'var(--color-red)';
  const okLabel = EDIT_ID ? 'PEDIDO ATUALIZADO' : 'PEDIDO CRIADO';
  titleEl.textContent = ok ? `✓  ${okLabel}  ·  HTTP ${status}` : `✕  ERRO  ·  HTTP ${status}`;
  bodyEl.textContent = typeof body === 'string' ? body : JSON.stringify(body, null, 2);
  panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

async function enviarPedido() {
  const btn = document.getElementById('btn-submit');
  btn.disabled = true;
  btn.classList.add('loading');
  btn.textContent = 'PROCESSANDO...';

  try {
    const data = EDIT_ID
      ? await Api.put(`/api/pedidos/${EDIT_ID}`, buildPayload())
      : await Api.post('/api/pedidos', buildPayload());



    showResponse(true, EDIT_ID ? 200 : 201, data);
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
    btn.textContent = SUBMIT_LABEL;
  }
}

const SUBMIT_LABEL = EDIT_ID ? 'SALVAR ALTERAÇÕES' : 'ENVIAR PEDIDO';

// Prefill do formulário a partir do pedido em edição (mesmos value=int dos selects).
function prefillEdit(pedido) {
  tipoPedidoEl.value = String(pedido.tipoPedido);
  document.getElementById('cor-tampa').value = String(pedido.corTampa);
  if (ordemProducaoEl) ordemProducaoEl.value = String(pedido.ordemProducao ?? '');
  syncBlocos(); // ativa os blocos do tipo e zera as lâminas

  (pedido.blocos ?? []).forEach((b, idx) => {
    const card = document.getElementById(BLOCO_IDS[idx]);
    if (!card) return;
    card.querySelector('.cor-bloco-sel').value = String(b.cor);

    const addBtn = card.querySelector('.btn-add-lam');
    (b.laminas ?? []).forEach((l) => {
      addLamina(addBtn);
      const row = card.querySelector('.laminas-list .lamina-row:last-child');
      if (!row) return;
      row.querySelector('.lam-cor').value = String(l.cor);
      row.querySelector('.lam-pad').value = String(l.padrao);
      row.querySelector('.lam-pos').value = String(l.posicaoNoBloco); // resposta usa posicaoNoBloco
    });
  });
  updatePreview();
}

function limparForm() {
  tipoPedidoEl.selectedIndex = 0;
  document.getElementById('cor-tampa').selectedIndex = 0;
  if (ordemProducaoEl) ordemProducaoEl.value = OP_SUGERIDA;

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


// ─── Eventos ─────────────────────────────────────────────────────────────────

tipoPedidoEl.addEventListener('change', syncBlocos);
document.getElementById('cor-tampa').addEventListener('change', updatePreview);
document.getElementById('btn-submit').addEventListener('click', enviarPedido);
document.getElementById('btn-clear').addEventListener('click', limparForm);
document.querySelectorAll('.btn-add-lam').forEach((btn) => {
  btn.addEventListener('click', () => addLamina(btn));
});

// Delegação para cor dos blocos — atualiza preview ao mudar qualquer select de cor
document.getElementById('blocos-container')?.addEventListener('change', updatePreview);

// ─── Init ────────────────────────────────────────────────────────────────────

if (EDIT_ID) {
  document.getElementById('btn-submit').textContent = SUBMIT_LABEL;
  const titulo = document.querySelector('.page-title');
  if (titulo) titulo.innerHTML = 'Editar Pedido <span>Smart 4.0</span>';
  prefillEdit(PEDIDO_EDIT);
} else {
  syncBlocos(); // chama updatePreview internamente
}


if (!TAMPA_HABILITADA) {
  document.getElementById('cor-tampa').selectedIndex = 0;

  document.getElementById("cor-tampa").closest(".field").style.display = 'none'
}
