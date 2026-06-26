// Tela "Estações": status ao vivo (evento `estacao-status`) + TODOS os dados da bancada
// (evento `estacao-all` = bean *CLP completo) por estação, com o componente visual da bancada
// no topo. Consumidor SSE puro — não acessa o banco nem chama /api/**.
import { createSse } from '../core/sse.js';
import bancadaStatus from '../components/bancadaStatus.js';

// estado (off/on/pause) → [rótulo, variante de badge]. Default = "Sem leitura"/dim.
const ESTADO = {
  off:   ['Desligado', 'red'],
  on:    ['Ligado', 'green'],
  pause: ['Pausado', 'yellow'],
};

// funcionamento (0/1/2 ou null) → rótulo da operação em curso.
const FUNC = { 0: 'Ocupado', 1: 'Iniciando', 2: 'Finalizando' };

// Status mínimo (estacao-status): badge + operação do card + overlays do componente da bancada.
// `estacao` é o frontKey, que é o id do card e a chave do bancadaStatus.
function renderStatus(d) {
  const card = document.getElementById(d.estacao);
  if (card) {
    card.dataset.lendo = '1';

    console.log(d.estado)
    const [txt, cor] = ESTADO[d.estado] || ['Sem leitura', 'dim'];
    const badge = card.querySelector('.estado');
    badge.textContent = txt;
    badge.className = `estacao-card__badge badge badge--${cor} estado`;

    const func = (d.funcionamento === null || d.funcionamento === undefined)
      ? '—'
      : (FUNC[d.funcionamento] ?? d.funcionamento);
    card.querySelector('.func').textContent = func;
  }

  // Componente da bancada no topo da página.
  bancadaStatus.setEstado(d.estacao, d.estado);
  bancadaStatus.setFuncionamento(d.estacao, d.funcionamento);
}

// camelCase / snake_case → rótulo legível.
const rotulo = (k) => k
  .replace(/_/g, ' ')
  .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
  .replace(/^./, (c) => c.toUpperCase());

// Valor do bean → texto exibível (boolean → Sim/Não, array → lista, nulo → —).
function valor(v) {
  if (typeof v === 'boolean') return v ? 'Sim' : 'Não';
  if (Array.isArray(v)) return v.length ? v.join(', ') : '—';
  return (v === null || v === undefined) ? '—' : String(v);
}

// Frescor por estação: o estacao-all é gateado no back (só chega enquanto há comunicação CLP). Sem
// evento por mais que LIMITE_MS → a comunicação está parada e marcamos o card como "aguardando".
const ultimaLeitura = {};
const LIMITE_MS = 2500;

// Dados completos (estacao-all): renderiza TODOS os campos do bean *CLP num grid chave/valor.
// Genérico — não hardcoda o schema de cada estação; itera o que o backend mandar.
function renderDados(d) {
  const card = document.getElementById(d.estacao);
  if (!card) return;
  const box = card.querySelector('.dados');
  if (!box) return;

  ultimaLeitura[d.estacao] = Date.now();
  card.dataset.comunicacao = 'on';

  const entries = Object.entries(d.dados || {});
  box.innerHTML = entries.length
    ? entries
        .map(([k, v]) =>
          `<div class="dado"><span class="dado__k">${rotulo(k)}</span><span class="dado__v">${valor(v)}</span></div>`)
        .join('')
    : '<div class="dado dado--vazio">Sem dados.</div>';
}



const sse = createSse();
sse.on('estacao-status', renderStatus); // status + overlays da bancada
sse.on('estacao-all', renderDados);     // dados completos do bean *CLP
sse.connect();
