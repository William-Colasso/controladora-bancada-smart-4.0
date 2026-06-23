import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';
import { createSse } from '../core/sse.js';
import { createPoller } from '../core/poller.js';
import bancadaStatus from '../components/bancadaStatus.js';

// apiName (path REST / id da linha) → frontKey (chave dos overlays e dos eventos SSE).
// Só PROCESSO difere: "processo" → "producao". Resto é igual.
const API_TO_FRONT = { processo: 'producao' };
const frontKey = (apiName) => API_TO_FRONT[apiName] || apiName;

// Atualiza o badge de status de conexão dentro de uma linha de estação (.clp-ip-input).
function setClpBadge(row, status, label, icon) {
  const badge = row.querySelector('.clp-status-badge');
  if (!badge) return;
  badge.className = `clp-status-badge badge badge--${status}`;
  badge.innerHTML = `<i class="fa-solid ${icon}"></i> ${label}`;
}

// Alterna o rótulo/ícone do botão entre Conectar e Desconectar.
function setBotao(button, conectado) {
  button.innerHTML = conectado
    ? '<i class="fa-solid fa-plug-circle-xmark"></i> Desconectar'
    : '<i class="fa-solid fa-plug"></i> Conectar';
}

// Conectar: grava IP, testa o CLP (S7 :102) e habilita a leitura read-only daquela estação.
// A estação é derivada do id da linha pai: "estoque-clp-ip" → "estoque" (apiName).
async function conectarClp(button, row, estacao) {
  const input = row.querySelector('input.ip-clp');
  const ip = input.value.trim();
  if (!ip) {
    Toast.error('Informe o IP do CLP.');
    return;
  }

  setClpBadge(row, 'dim', 'Conectando...', 'fa-circle-notch fa-spin');
  try {
    const r = await Api.post(`/api/clp/${estacao}/conectar`, { ip });
    if (r.alcancavel && r.leitura) {
      row.dataset.conectado = '1';
      setBotao(button, true);
      setClpBadge(row, 'green', 'Lendo', 'fa-circle-check');
      Toast.success(`Estação ${estacao} conectada: ${ip}`);
    } else {
      setClpBadge(row, 'red', 'CLP não responde', 'fa-circle-xmark');
      Toast.error(`CLP da estação ${estacao} (${ip}) não respondeu na porta 102.`);
    }
  } catch (err) {
    setClpBadge(row, 'red', 'Falha', 'fa-circle-xmark');
    Toast.error(err.message || 'Falha ao conectar.');
  }
}

// Desconectar: para a leitura da estação e apaga o overlay correspondente.
async function desconectarClp(button, row, estacao) {
  try {
    await Api.post(`/api/clp/${estacao}/desconectar`);
    delete row.dataset.conectado;
    setBotao(button, false);
    setClpBadge(row, 'dim', 'Desconectado', 'fa-circle-question');
    const fk = frontKey(estacao);
    bancadaStatus.setEstado(fk, 'off');
    bancadaStatus.setFuncionamento(fk, null);
  } catch (err) {
    Toast.error(err.message || 'Falha ao desconectar.');
  }
}

document.querySelectorAll('.btn-conectar-clp').forEach((button) => {
  const row = button.closest('.clp-ip-input');
  const estacao = row.id.replace('-clp-ip', '');
  button.addEventListener('click', async () => {
    button.disabled = true;
    try {
      if (row.dataset.conectado) await desconectarClp(button, row, estacao);
      else await conectarClp(button, row, estacao);
    } finally {
      button.disabled = false;
    }
  });
});

// Status das estações da bancada em tempo real (SSE). Alimenta os overlays do bancada-status.
const sse = createSse();
sse.on('estacao-status', (d) => {
  bancadaStatus.setEstado(d.estacao, d.estado);
  bancadaStatus.setFuncionamento(d.estacao, d.funcionamento);
});
sse.connect();

// Driver de escrita (lado escrita do loop CLP↔Backend↔Frontend). Enquanto ligado, repete o
// handshake POST /api/clp/processar — o caller é quem repete a passada. O reflexo visual do novo
// estado chega pelos eventos SSE acima; nada é renderizado a partir da resposta do POST.
const btnComunicacao = document.getElementById('btn-toggle-comunicacao');
if (btnComunicacao) {
  let ligado = false;
  const poller = createPoller(() => Api.post('/api/clp/processar'), 1000, () => {});

  btnComunicacao.addEventListener('click', () => {
    ligado = !ligado;
    if (ligado) {
      poller.start();
      btnComunicacao.classList.add('btn--danger');
      btnComunicacao.innerHTML = '<i class="fa-solid fa-stop"></i> Parar comunicação CLP';
    } else {
      poller.stop();
      btnComunicacao.classList.remove('btn--danger');
      btnComunicacao.innerHTML = '<i class="fa-solid fa-play"></i> Iniciar comunicação CLP';
    }
  });
}
