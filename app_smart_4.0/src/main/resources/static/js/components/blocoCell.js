import { COR_INT_TO_NAME } from '../core/enums.js';
import { formatOP } from '../core/format.js';

const COR_NAME_TO_CLASS = {
  VAZIO: 'bloco--vazio',
  PRETO: 'bloco--preto',
  VERMELHO: 'bloco--vermelho',
  AZUL: 'bloco--azul',
};

const COR_CLASSES = Object.values(COR_NAME_TO_CLASS);

export function createEstoqueCell(pos, onToggle) {
  const div = document.createElement('div');
  div.id = `bloco-est-${pos}`;
  div.className = 'bloco bloco--estoque bloco--vazio';
  div.addEventListener('click', () => onToggle(pos));
  return div;
}

export function createExpedicaoCell(pos) {
  const div = document.createElement('div');
  div.id = `bloco-exp-${pos}`;
  div.className = 'bloco bloco--vazio';
  return div;
}

export function renderEstoqueCell(cell, pos, corInt, selected) {
  const corName = COR_INT_TO_NAME[corInt] ?? 'VAZIO';
  cell.classList.remove(...COR_CLASSES, 'skeleton', 'bloco--selected');
  cell.classList.add(COR_NAME_TO_CLASS[corName] || 'bloco--vazio');
  if (selected) cell.classList.add('bloco--selected');
  cell.innerHTML = `<span class="bloco__pos">${pos}</span>`;
}

// Célula somente-leitura do grid "Estoque (CLP)" — espelha a do estoque, mas sem clique/seleção
// (não tem a classe bloco--estoque, então não recebe cursor/hover de seleção).
export function createClpEstoqueCell(pos) {
  const div = document.createElement('div');
  div.id = `bloco-clp-${pos}`;
  div.className = 'bloco bloco--vazio';
  return div;
}

// Renderiza a cor lida do CLP e marca divergência (cor diferente da do banco) com bloco--divergente.
export function renderClpEstoqueCell(cell, pos, corInt, divergente) {
  const corName = COR_INT_TO_NAME[corInt] ?? 'VAZIO';
  cell.classList.remove(...COR_CLASSES, 'skeleton', 'bloco--divergente');
  cell.classList.add(COR_NAME_TO_CLASS[corName] || 'bloco--vazio');
  if (divergente) cell.classList.add('bloco--divergente');
  cell.innerHTML = `<span class="bloco__pos">${pos}</span>`;
}

export function renderExpedicaoCell(cell, pos, pedido) {
  cell.classList.remove('bloco--vazio', 'bloco--ocupado', 'skeleton');
  if (pedido) {
    cell.classList.add('bloco--ocupado');
    cell.innerHTML = `
      <span class="bloco__pos">${pos}</span>
      <span class="exp-bloco__op">OP ${formatOP(pedido.ordemProducao)}</span>`;
  } else {
    cell.classList.add('bloco--vazio');
    cell.innerHTML = `<span class="bloco__pos">${pos}</span>`;
  }
}
