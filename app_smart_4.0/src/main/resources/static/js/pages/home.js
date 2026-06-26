import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';
import { createSse } from '../core/sse.js';
import bancadaStatus from '../components/bancadaStatus.js';

// Atualiza o badge de status dentro de uma linha de estação (.clp-ip-input).
function setClpBadge(row, status, label, icon) {
  const badge = row.querySelector('.clp-status-badge');
  if (!badge) return;
  badge.className = `clp-status-badge badge badge--${status}`;
  badge.innerHTML = `<i class="fa-solid ${icon}"></i> ${label}`;
}

// Pré-preenche os inputs com o IP atual de cada estação (GET /api/clp/ips).
async function carregarIps() {
  try {
    const ips = await Api.get('/api/clp/ips'); // [{ estacao, ip }]
    ips.forEach(({ estacao, ip }) => {
      const row = document.getElementById(`${estacao}-clp-ip`);
      if (row && ip) row.querySelector('input.ip-clp').value = ip;
    });
  } catch (_) { /* sem IPs salvos ainda — segue com os campos vazios */ }
}

// "Tela de conexão" = apenas grava o IP da estação (PUT /api/clp/ips/{estacao}).
// A leitura do CLP NÃO é mais ligada aqui: os produtores SSE leem sozinhos sempre que houver ao
// menos 1 cliente SSE conectado (qualquer tela aberta). Ver core/sse.js + SseEmitterRegistry no back.
// A estação vem do id da linha pai: "estoque-clp-ip" → "estoque" (apiName).
async function salvarIp(row, estacao) {
  const input = row.querySelector('input.ip-clp');
  const ip = input.value.trim();
  if (!ip) {
    Toast.error('Informe o IP do CLP.');
    return;
  }

  setClpBadge(row, 'dim', 'Salvando...', 'fa-circle-notch fa-spin');
  try {
    const r = await Api.put(`/api/clp/ips/${estacao}`, { ip });
    setClpBadge(row, 'green', 'IP salvo', 'fa-circle-check');
    Toast.success(`IP da estação ${estacao} salvo: ${r.ip}`);
  } catch (err) {
    setClpBadge(row, 'red', 'IP inválido', 'fa-circle-xmark');
    Toast.error(err.message || 'Falha ao salvar o IP.');
  }
}

document.querySelectorAll('.btn-conectar-clp').forEach((button) => {
  const row = button.closest('.clp-ip-input');
  const estacao = row.id.replace('-clp-ip', '');
  button.addEventListener('click', async () => {
    button.disabled = true;
    try {
      await salvarIp(row, estacao);
    } finally {
      button.disabled = false;
    }
  });
});

carregarIps();

// Status das estações da bancada em tempo real (SSE). Alimenta os overlays do bancada-status.
// Abrir esta tela já basta para o back-end ler E processar os CLPs automaticamente, a cada 300ms
// (gating por cliente SSE — ver ClpProcessamentoScheduler). Não há mais botão de "iniciar comunicação".
const statusComunicacao = document.getElementById('comunicacao-status');
let ultimaLeitura = 0;

// Badge informativo: reflete se o CLP está sendo lido de fato. Cada estacao-all que chega é um
// "heartbeat" de leitura viva; sem nenhum por >2,5s → leitura parada (IP não configurado, CLP fora…).
function setLeituraBadge(ativa) {
  if (!statusComunicacao) return;
  statusComunicacao.className = `badge badge--${ativa ? 'green' : 'dim'}`;
  statusComunicacao.innerHTML = ativa
    ? '<i class="fa-solid fa-circle"></i> Leitura automática ativa'
    : '<i class="fa-solid fa-circle-notch"></i> Sem leitura do CLP';
}
setLeituraBadge(false);

const sse = createSse();
sse.on('estacao-status', (d) => {
  bancadaStatus.setEstado(d.estacao, d.estado);
  bancadaStatus.setFuncionamento(d.estacao, d.funcionamento);
});
sse.on('estacao-all', () => { ultimaLeitura = Date.now(); }); // heartbeat de leitura para o badge
sse.connect();

setInterval(() => setLeituraBadge(Date.now() - ultimaLeitura <= 2500), 1000);
