// Tela "Estações": status ao vivo (evento `estacao-status`) + TODOS os dados da bancada
// (evento `estacao-all` = bean *CLP completo) por estação, com o componente visual da bancada
// no topo. Consumidor SSE puro — não acessa o banco nem chama /api/**.
import { createSse } from '../core/sse.js';
import bancadaStatus from '../components/bancadaStatus.js';
import { createPedidoViewer } from '../components/pedidoViewer.js';
import { buildDetailHTML } from '../components/pedidoDetail.js';
import { initCommBanner } from '../components/commBanner.js';
import { Api } from '../core/api.js';
import { Toast } from '../core/toast.js';

// estado (off/on/pause) → [rótulo, variante de badge]. Default = "Sem leitura"/dim.
const ESTADO = {
  off:   ['Desligado', 'red'],
  on:    ['Ligado', 'green'],
  pause: ['Pausado', 'yellow'],
};

// funcionamento (0/1/2 ou null) → rótulo da operação em curso.
const FUNC = { 0: 'Ocupado', 1: 'Iniciando', 2: 'Finalizando' };

// Frescor por estação: a liveness vem do estacao-heartbeat (um pulso por passada lida, mesmo com a
// estação ociosa). Sem pulso por mais que LIMITE_MS → comunicação parada → card "aguardando".
const ultimaLeitura = {};
const LIMITE_MS = 2500;

// Funcionamento atual por estação (0/1/2 ou null). Guardado para re-renderizar o overlay da bancada
// quando o heartbeat chega/para — a cor depende da vivacidade, não só do último estacao-status.
const funcAtual = {};

// "Sem dados ainda" é diferente de "perdemos o sinal": undefined → assume vivo até o watchdog provar
// o contrário (ou seja, só pune após receber ao menos um heartbeat e depois perdê-lo).
const viva = (estacao) =>
  ultimaLeitura[estacao] === undefined
  || Date.now() - ultimaLeitura[estacao] <= LIMITE_MS;

// Overlay da bancada = vivacidade do heartbeat (≤ LIMITE_MS) + funcionamento atual.
function renderBancada(estacao) {
  bancadaStatus.aplicar(estacao, viva(estacao), funcAtual[estacao] ?? null);
}

// Status mínimo (estacao-status): badge + operação do card + overlays do componente da bancada.
// `estacao` é o frontKey, que é o id do card e a chave do bancadaStatus.
function renderStatus(d) {
  console.log('estado recebido:', d.estado, '| viva:', viva(d.estacao));
  const card = document.getElementById(d.estacao);
  if (card) {
    card.dataset.lendo = '1';

    const chave = viva(d.estacao) ? d.estado : null;
    const [txt, cor] = ESTADO[chave] ?? ['Sem leitura', 'dim'];
    const badge = card.querySelector('.estado');
    badge.textContent = txt;
    badge.className = `estacao-card__badge badge badge--${cor} estado`;

    const func = (d.funcionamento === null || d.funcionamento === undefined)
      ? '—'
      : (FUNC[d.funcionamento] ?? d.funcionamento);
    card.querySelector('.func').textContent = func;
  }

  // Componente da bancada no topo da página. A cor (verde/vermelho) vem da vivacidade do heartbeat,
  // não do `estado` — idle conectado emite estado='off'. Aqui só guardamos o funcionamento e
  // reavaliamos; o pulso/watchdog é quem decide ligada (verde) x desligada (vermelho).
  funcAtual[d.estacao] = d.funcionamento;
  renderBancada(d.estacao);
}

// Rótulos PT dos campos conhecidos do bean *CLP; fallback = prettify genérico do camelCase
// (mantém o painel agnóstico a schema — campos novos aparecem sem precisar de mapa).
const LABELS = {
  recebidoOp: 'Recebido OP', numeroOP: 'Número OP',
  finishOP: 'Finalizou OP', startOP: 'Iniciou OP', cancelOP: 'Cancelou OP',
  ocupado: 'Ocupado', aguardando: 'Aguardando', manual: 'Manual', emergencia: 'Emergência',
  recebidoExpedicao: 'Recebido Exp.', iniciarGuardarExp: 'Iniciar guardar',
  posicaoGuardarExp: 'Posição guardar', orderExpedicao: 'Ordem expedição',
  pedirPosicaoExp: 'Pedir posição', posicaoGuardadoExpedicao: 'Posição guardada',
  posicaoRemovidoExpedicao: 'Posição removida', adicionarExpedicao: 'Adicionar',
  removerExpedicao: 'Remover', opGuardadoExpedicao: 'OP guardada',
};
const rotulo = (k) => LABELS[k] ?? k
  .replace(/_/g, ' ')
  .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
  .replace(/^./, (c) => c.toUpperCase());

// Valor não-booleano do bean → texto exibível (array → lista, nulo → —).
function valor(v) {
  if (Array.isArray(v)) return v.length ? v.join(', ') : '—';
  return (v === null || v === undefined) ? '—' : String(v);
}

// Painel HMI: separa SINAIS (booleanos, como LEDs) de VALORES (números/arrays, como readout).
// Continua genérico — itera o que o bean trouxer, sem hardcodar o schema de cada estação.
function buildHmi(dados) {
  const entries = Object.entries(dados || {});
  if (!entries.length) return '<div class="dado dado--vazio">Sem dados.</div>';

  const sinais = entries.filter(([, v]) => typeof v === 'boolean');
  const valores = entries.filter(([, v]) => typeof v !== 'boolean');

  const led = ([k, v]) => {
    const alerta = k.toLowerCase().includes('emergencia') && v ? ' data-alerta="1"' : '';
    return `<span class="sinal" data-on="${v}"${alerta}>
        <span class="sinal__led"></span>${rotulo(k)}</span>`;
  };
  const readout = ([k, v]) =>
    `<div class="valor"><span class="valor__k">${rotulo(k)}</span>
       <span class="valor__dots"></span><span class="valor__v">${valor(v)}</span></div>`;

  const secaoSinais = sinais.length
    ? `<div class="hmi__sec"><span class="hmi__titulo">Sinais</span>
         <div class="sinais-grid">${sinais.map(led).join('')}</div></div>`
    : '';
  const secaoValores = valores.length
    ? `<div class="hmi__sec"><span class="hmi__titulo">Valores</span>
         <div class="valores">${valores.map(readout).join('')}</div></div>`
    : '';
  return secaoSinais + secaoValores;
}

// OP em execução por estação (numeroOP do bean *CLP). Alimenta a linha do card + o botão "Ver pedido".
const opAtual = {};

// Dados completos (estacao-all): renderiza TODOS os campos do bean *CLP num grid chave/valor.
// Genérico — não hardcoda o schema de cada estação; itera o que o backend mandar.
function renderDados(d) {
  const card = document.getElementById(d.estacao);
  if (!card) return;

  // OP em execução: destaca o numeroOP e habilita o popup do pedido quando > 0.
  const op = d.dados?.numeroOP ?? 0;
  opAtual[d.estacao] = op;
  const opEl = card.querySelector('.op-atual');
  if (opEl) opEl.textContent = op > 0 ? op : '—';
  const verBtn = card.querySelector('.ver-pedido-btn');
  if (verBtn) verBtn.hidden = !(op > 0);

  const box = card.querySelector('.dados');
  if (!box) return;

  box.innerHTML = buildHmi(d.dados);
}

// ─── Popup do pedido em execução (3D + detalhes) ──────────────────────────────
const modal = document.getElementById('pedido-modal');
let modalViewer = null;
const getModalViewer = () =>
  (modalViewer ??= createPedidoViewer(document.getElementById('modal-viewer')));

async function abrirPedido(op) {
  try {
    // ponytail: lista pequena (poucas OPs ativas), filtra no cliente; criar GET /api/pedidos/op/{op} se crescer.
    const pedidos = await Api.get('/api/pedidos');
    const pedido = (pedidos ?? []).find((p) => p.ordemProducao === op);
    if (!pedido) { Toast.info(`Pedido da OP ${op} não encontrado.`); return; }
    document.getElementById('modal-info').innerHTML = buildDetailHTML(pedido);
    getModalViewer().update(pedido);
    modal.showModal();
  } catch (err) {
    Toast.error(err.message);
  }
}

document.querySelector('.estacoes__grid')?.addEventListener('click', (e) => {
  const btn = e.target.closest('.ver-pedido-btn');
  if (!btn) return;
  const op = opAtual[btn.closest('.estacao-card')?.id];
  if (op > 0) abrirPedido(op);
});
document.getElementById('modalClose')?.addEventListener('click', () => modal.close());
modal?.addEventListener('click', (e) => { if (e.target === modal) modal.close(); }); // clique no backdrop fecha


const sse = createSse();
sse.on('estacao-status', renderStatus); // status + overlays da bancada
sse.on('estacao-all', renderDados);     // dados completos do bean *CLP (on-change)

// Liveness por estação: cada estacao-heartbeat = leitura viva (mesmo com a estação ociosa, quando o
// estacao-all não muda). Marca o card como comunicando; o watchdog abaixo o derruba se o pulso parar.
sse.on('estacao-heartbeat', (d) => {
  ultimaLeitura[d.estacao] = Date.now();
  const card = document.getElementById(d.estacao);
  if (card) card.dataset.comunicacao = 'on';
  renderBancada(d.estacao); // pulso vivo → reavalia a cor (sai de vermelho p/ verde quando volta a ler)
});

initCommBanner(sse); // banner global além do watchdog por card
sse.connect();

// Watchdog: sem pulso de uma estação por mais que LIMITE_MS → comunicação parada → "aguardando".
// Não chama renderStatus (que espera um evento SSE completo) — só atualiza o dataset e a bancada.
setInterval(() => {
  const agora = Date.now();
  Object.keys(ultimaLeitura).forEach((estacao) => {
    if (agora - ultimaLeitura[estacao] > LIMITE_MS) {
      const card = document.getElementById(estacao);
      if (card) {
        card.dataset.comunicacao = 'off';
        // Atualiza o badge para "Sem leitura" diretamente, sem precisar de um evento SSE.
        const badge = card.querySelector('.estado');
        if (badge) {
          badge.textContent = 'Sem leitura';
          badge.className = 'estacao-card__badge badge badge--dim estado';
        }
      }
      renderBancada(estacao); // sem pulso → bancada fica vermelha
    }
  });
}, 1000);