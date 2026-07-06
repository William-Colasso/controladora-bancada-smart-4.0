// Página CONFIGURAÇÃO — comunicação com os CLPs e controladora de tampa.
// Rede: base (3 octetos) compartilhada + final por estação; salvar compõe o IP completo e faz
// PUT /api/clp/ips/{estacao}. Tampa: GET/PUT /api/config/tampa (toggle + IP).
import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';

const baseInput = document.getElementById('rede-base');
const rows = [...document.querySelectorAll('.config-row--estacao')];
const salvarTodosBtn = document.getElementById('salvarTodosBtn');

const tampaSwitch = document.getElementById('tampa-switch');
const tampaIp = document.getElementById('tampa-ip');
const tampaBadge = document.getElementById('tampaBadge');
const salvarTampaBtn = document.getElementById('salvarTampaBtn');

function setBadge(el, status, label, icon) {
  if (!el) return;
  el.className = `config-badge badge badge--${status}`;
  el.innerHTML = `<i class="fa-solid ${icon}"></i> ${label}`;
}

function baseAtual() {
  return baseInput.value.trim().replace(/\.+$/, '');
}

function sincronizarPreviews() {
  const base = baseAtual() || '—';
  document.querySelectorAll('.config-preview__base').forEach((el) => { el.textContent = base; });
}

// ── Carga inicial: IPs atuais → base (da 1ª estação) + finais por estação ────
async function carregarIps() {
  try {
    const ips = await Api.get('/api/clp/ips'); // [{ estacao, ip }]
    const porEstacao = Object.fromEntries(ips.map(({ estacao, ip }) => [estacao, ip || '']));

    // Base compartilhada: 3 primeiros octetos do primeiro IP configurado.
    const primeiro = ips.find(({ ip }) => ip)?.ip;
    if (primeiro) baseInput.value = primeiro.split('.').slice(0, 3).join('.');

    rows.forEach((row) => {
      const est = row.dataset.estacao;
      const finalInput = row.querySelector('.config-input--final');
      const atual = porEstacao[est];
      finalInput.value = atual ? atual.split('.')[3] : row.dataset.sugestao;
      const badge = row.querySelector('.config-badge');
      setBadge(badge, 'dim', atual || 'sem IP', atual ? 'fa-circle-check' : 'fa-circle-question');
    });
  } catch (_) {
    rows.forEach((row) => {
      row.querySelector('.config-input--final').value = row.dataset.sugestao;
    });
  }
  sincronizarPreviews();
}

async function salvarEstacao(row) {
  const est = row.dataset.estacao;
  const badge = row.querySelector('.config-badge');
  const fim = row.querySelector('.config-input--final').value.trim();
  const base = baseAtual();

  if (!base || !fim) {
    setBadge(badge, 'red', 'preencha base e final', 'fa-circle-xmark');
    return false;
  }

  const ip = `${base}.${fim}`;
  setBadge(badge, 'dim', 'salvando…', 'fa-circle-notch fa-spin');
  try {
    const r = await Api.put(`/api/clp/ips/${est}`, { ip });
    setBadge(badge, 'green', r.ip, 'fa-circle-check');
    return true;
  } catch (err) {
    setBadge(badge, 'red', err.message || 'IP inválido', 'fa-circle-xmark');
    return false;
  }
}

baseInput.addEventListener('input', sincronizarPreviews);

rows.forEach((row) => {
  row.querySelector('.btn-salvar-ip').addEventListener('click', async (e) => {
    e.currentTarget.disabled = true;
    try {
      await salvarEstacao(row);
    } finally {
      e.currentTarget && (e.currentTarget.disabled = false);
    }
  });
});

if (salvarTodosBtn) {
  salvarTodosBtn.addEventListener('click', async () => {
    salvarTodosBtn.disabled = true;
    let ok = 0;
    for (const row of rows) {
      if (await salvarEstacao(row)) ok++;
    }
    salvarTodosBtn.disabled = false;
    if (ok === rows.length) Toast.success('IPs das 4 estações salvos');
    else Toast.error(`${rows.length - ok} estação(ões) com erro — veja os badges`);
  });
}

// ── Tampa (ESP32) ────────────────────────────────────────────────────────────
async function carregarTampa() {
  try {
    const cfg = await Api.get('/api/config/tampa'); // { habilitada, ip }
    tampaSwitch.checked = !!cfg.habilitada;
    tampaIp.value = cfg.ip || '';
    setBadge(tampaBadge, cfg.habilitada ? 'green' : 'dim',
      cfg.habilitada ? 'ativa' : 'desativada',
      cfg.habilitada ? 'fa-circle-check' : 'fa-circle-minus');
  } catch (_) { /* config indisponível — campos ficam vazios */ }
}

if (salvarTampaBtn) {
  salvarTampaBtn.addEventListener('click', async () => {
    salvarTampaBtn.disabled = true;
    setBadge(tampaBadge, 'dim', 'salvando…', 'fa-circle-notch fa-spin');
    try {
      const cfg = await Api.put('/api/config/tampa', {
        habilitada: tampaSwitch.checked,
        ip: tampaIp.value.trim(),
      });
      setBadge(tampaBadge, cfg.habilitada ? 'green' : 'dim',
        cfg.habilitada ? `ativa · ${cfg.ip}` : 'desativada',
        cfg.habilitada ? 'fa-circle-check' : 'fa-circle-minus');
      Toast.success('Configuração da tampa salva');
    } catch (err) {
      setBadge(tampaBadge, 'red', err.message || 'IP inválido', 'fa-circle-xmark');
      Toast.error(err.message || 'Falha ao salvar a tampa');
    } finally {
      salvarTampaBtn.disabled = false;
    }
  });
}

carregarIps();
carregarTampa();
