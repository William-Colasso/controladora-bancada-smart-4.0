// Tela "Estações" — consumidor SSE puro do status ao vivo das 4 estações (evento `estacao-status`).
// Não acessa o banco nem chama /api/**; só escuta o CLP via SSE e pinta os cards.
import { createSse } from '../core/sse.js';

// estado (off/on/pause) → [rótulo, variante de badge]. Default = "Sem leitura"/dim.
const ESTADO = {
  off:   ['Desligado', 'red'],
  on:    ['Ligado', 'green'],
  pause: ['Pausado', 'yellow'],
};

// funcionamento (0/1/2 ou null) → rótulo da operação em curso.
const FUNC = { 0: 'Ocupado', 1: 'Iniciando', 2: 'Finalizando' };

// Atualiza um card a partir de um EstacaoStatusEvent {estacao, estado, funcionamento}.
// `estacao` é o frontKey, que é exatamente o id do card.
function render(d) {
  const card = document.getElementById(d.estacao);
  if (!card) return;

  card.dataset.lendo = '1';

  const [txt, cor] = ESTADO[d.estado] || ['Sem leitura', 'dim'];
  const badge = card.querySelector('.estado');
  badge.textContent = txt;
  badge.className = `estacao-card__badge badge badge--${cor} estado`;

  const func = (d.funcionamento === null || d.funcionamento === undefined)
    ? '—'
    : (FUNC[d.funcionamento] ?? d.funcionamento);
  card.querySelector('.func').textContent = func;
}

const sse = createSse();
sse.on('estacao-status', render);
sse.connect();
