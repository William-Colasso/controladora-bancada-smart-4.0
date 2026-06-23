import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';
import { createSse } from '../core/sse.js';
import { createPoller } from '../core/poller.js';
import bancadaStatus from '../components/bancadaStatus.js';

// Atualiza o badge de status de conexão dentro de uma linha de estação (.clp-ip-input).
function setClpBadge(row, status, label, icon) {
  const badge = row.querySelector('.clp-status-badge');
  if (!badge) return;
  badge.className = `clp-status-badge badge badge--${status}`;
  badge.innerHTML = `<i class="fa-solid ${icon}"></i> ${label}`;
}

// Define o IP do CLP da estação no registry (PUT /api/clp/ips/{estacao}).
// A estação é derivada do id da linha pai: "estoque-clp-ip" → "estoque".
async function conectarClp(button) {
  const row = button.closest('.clp-ip-input');
  const input = row.querySelector('input.ip-clp');
  const estacao = row.id.replace('-clp-ip', '');
  const ip = input.value.trim();

  if (!ip) {
    Toast.error('Informe o IP do CLP.');
    return;
  }

  button.disabled = true;
  setClpBadge(row, 'dim', 'Conectando...', 'fa-circle-notch fa-spin');

  try {
    await Api.put(`/api/clp/ips/${estacao}`, { ip });
    setClpBadge(row, 'green', 'OK', 'fa-circle-check');
    Toast.success(`IP da estação ${estacao} definido: ${ip}`);
  } catch (err) {
    setClpBadge(row, 'red', 'Falha', 'fa-circle-xmark');
    Toast.error(err.message || 'Falha ao definir o IP.');
  } finally {
    button.disabled = false;
  }
}

document.querySelectorAll('.btn-conectar-clp').forEach((button) => {
  button.addEventListener('click', () => conectarClp(button));
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
