const DIR = '/img/bancada/';
const BASE = 'Smart40.png';

// abrev → família de ESTADO (Smart40-Est_on.png, separador '-')
// nome  → família de FUNCIONAMENTO (Smart40_Estoque_1.png, separador '_')
const ESTACOES = {
  estoque:   { abrev: 'Est', nome: 'Estoque' },
  processo:  { abrev: 'Pro', nome: 'Processo' },
  montagem:  { abrev: 'Mon', nome: 'Montagem' },
  expedicao: { abrev: 'Exp', nome: 'Expedicao' },
};

const ESTADOS = ['off', 'on', 'pause'];   // sempre um destes
const FUNCIONAMENTOS = [0, 1, 2];          // ou null = camada oculta

// ---- builders de nome de arquivo ----
const nomeEstado        = (est, status) => `Smart40-${ESTACOES[est].abrev}_${status}.png`;
const nomeFuncionamento = (est, n)      => `Smart40_${ESTACOES[est].nome}_${n}.png`;
const srcEstado         = (est, status) => DIR + nomeEstado(est, status);
const srcFuncionamento  = (est, n)      => DIR + nomeFuncionamento(est, n);

// ---- troca de src no DOM ----
const elEstado = (est) => document.getElementById(`bancada-status-${est}`);
const elFunc   = (est) => document.getElementById(`bancada-status-${est}-func`);

function setEstado(est, status) {
  const img = elEstado(est);
  if (img && ESTACOES[est] && ESTADOS.includes(status)) img.src = srcEstado(est, status);
}

function setFuncionamento(est, n) {
  const img = elFunc(est);
  if (!img || !ESTACOES[est]) return;
  if (n === null || n === undefined) {     // sem tarefa → oculta a camada
    img.hidden = true;
    img.removeAttribute('src');
  } else if (FUNCIONAMENTOS.includes(n)) {
    img.src = srcFuncionamento(est, n);
    img.hidden = false;
  }
}

// Regra visual combinada (3 cores). A imagem de funcionamento só aparece quando a estação está ocupada.
// A cor de "ligada" vem da VIVACIDADE DA COMUNICAÇÃO (heartbeat), NÃO do `estado` do backend: uma
// estação comunicando mas parada (idle) emite estado='off', então usar 'off' a deixaria vermelha mesmo
// ligada. O heartbeat pulsa a cada leitura, esteja a estação ocupada ou não.
//  - sem comunicação (heartbeat parou) → VERMELHO (off),   sem imagem
//  - ocupada (funcionamento ativo)     → AMARELO  (pause), COM a imagem
//  - ligada (comunicando, sem tarefa)  → VERDE    (on),    sem imagem
function aplicar(est, viva, funcionamento) {
  if (!ESTACOES[est]) return;
  const ocupada = funcionamento !== null && funcionamento !== undefined;
  if (!viva) {
    setEstado(est, 'off');                 // vermelho
    setFuncionamento(est, null);           // sem imagem
  } else if (ocupada) {
    setEstado(est, 'pause');               // amarelo
    setFuncionamento(est, funcionamento);  // com a imagem
  } else {
    setEstado(est, 'on');                  // verde
    setFuncionamento(est, null);           // sem imagem
  }
}

export default {
  base: BASE,
  estacoes: ESTACOES,
  estados: ESTADOS,
  funcionamentos: FUNCIONAMENTOS,
  nomeEstado,
  nomeFuncionamento,
  srcEstado,
  srcFuncionamento,
  setEstado,
  setFuncionamento,
  aplicar,
};
