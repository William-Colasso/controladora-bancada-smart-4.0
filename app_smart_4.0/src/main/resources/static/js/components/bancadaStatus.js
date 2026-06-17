const DIR = '/img/bancada/';
const BASE = 'Smart40.png';

// abrev → família de ESTADO (Smart40-Est_on.png, separador '-')
// nome  → família de FUNCIONAMENTO (Smart40_Estoque_1.png, separador '_')
const ESTACOES = {
  estoque:   { abrev: 'Est', nome: 'Estoque' },
  producao:  { abrev: 'Pro', nome: 'Processo' },
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
};
