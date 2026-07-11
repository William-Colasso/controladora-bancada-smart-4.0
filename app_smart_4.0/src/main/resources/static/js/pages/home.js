import { createSse } from '../core/sse.js';
import bancadaStatus from '../components/bancadaStatus.js';

// Os IPs dos CLPs (e a tampa) são configurados na página /configuracao — aqui a home só
// acompanha o status da bancada em tempo real.

// Status das estações da bancada em tempo real (SSE). Alimenta os overlays do bancada-status.
// Abrir esta tela já basta para o back-end ler E processar os CLPs automaticamente, a cada 300ms
// (gating por cliente SSE — ver ClpProcessamentoScheduler). Não há mais botão de "iniciar comunicação".
const statusComunicacao = document.getElementById('comunicacao-status');
let ultimaLeitura = 0;

// Badge informativo: reflete se o CLP está sendo lido de fato. Cada estacao-heartbeat que chega é um
// pulso de leitura viva (emitido pelo write path a cada passada lida); sem nenhum por >2,5s →
// leitura parada (IP não configurado, CLP fora…).
function setLeituraBadge(ativa) {
  if (!statusComunicacao) return;
  statusComunicacao.className = `badge badge--${ativa ? 'green' : 'dim'}`;
  statusComunicacao.innerHTML = ativa
    ? '<i class="fa-solid fa-circle"></i> Leitura automática ativa'
    : '<i class="fa-solid fa-circle-notch"></i> Sem leitura do CLP';
}
setLeituraBadge(false);

// Vivacidade do heartbeat e funcionamento POR estação — a cor do overlay (verde/vermelho) depende do
// heartbeat, não do `estado` (idle conectado emite estado='off'). Ver bancadaStatus.aplicar.
const LIMITE_MS = 2500;
const ultimaLeituraEst = {};
const funcAtual = {};

function renderBancada(estacao) {
  const viva = Date.now() - (ultimaLeituraEst[estacao] ?? 0) <= LIMITE_MS;
  bancadaStatus.aplicar(estacao, viva, funcAtual[estacao] ?? null);
}

const sse = createSse();
sse.on('estacao-status', (d) => {
  funcAtual[d.estacao] = d.funcionamento;
  renderBancada(d.estacao);
});
sse.on('estacao-heartbeat', (d) => {
  ultimaLeitura = Date.now();                 // badge global de leitura
  ultimaLeituraEst[d.estacao] = Date.now();   // vivacidade por estação (cor da bancada)
  renderBancada(d.estacao);
});
sse.connect();

setInterval(() => {
  setLeituraBadge(Date.now() - ultimaLeitura <= LIMITE_MS);
  Object.keys(ultimaLeituraEst).forEach(renderBancada); // sem pulso → estação vira vermelha
}, 1000);
