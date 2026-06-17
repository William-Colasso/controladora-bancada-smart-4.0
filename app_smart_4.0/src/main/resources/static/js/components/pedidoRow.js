import { corBlocoClass, statusBadgeClass, tipoChipClass } from '../core/enums.js';
import { formatCount, formatDateTime, tampaHex } from '../core/format.js';
import { patchText, patchInner } from '../core/dom.js';

const blocosSig = (blocos) => JSON.stringify(blocos);
const miniBloco = (b) => `<div class="mini-bloco mini-bloco--${corBlocoClass(b.cor)}"></div>`;

export function buildRowHTML(p) {
  const blocos = p.blocos ?? [];
  return `
    <td data-cell="id">#${formatCount(p.id)}</td>
    <td data-cell="op">${p.ordemProducao ?? '—'}</td>
    <td data-cell="status">
      <span class="badge ${statusBadgeClass(p.status)}">${p.status}</span>
    </td>
    <td data-cell="tipo">
      <span class="tipo-chip ${tipoChipClass(p.tipoPedido)}">${p.tipoPedido}</span>
    </td>
    <td data-cell="tampa">
      <div class="tampa-visual">
        <div class="tampa-swatch" style="background:${tampaHex(p.corTampa)}"></div>
        ${p.corTampa}
      </div>
    </td>
    <td data-cell="blocos">
      <div class="blocos-preview">${blocos.map(miniBloco).join('')}</div>
    </td>
    <td data-cell="criacao">${formatDateTime(p.dataCriacao)}</td>
    <td data-cell="expedicao">${formatDateTime(p.dataEntradaExpedicao)}</td>
    <td><button class="pedido-start-button" data-id="${p.id}"></button></td>`;
}

export function patchRow(row, next, prev) {
  const cell = (name) => row.querySelector(`[data-cell="${name}"]`);

  if (next.ordemProducao !== prev.ordemProducao) {
    patchText(cell('op'), next.ordemProducao ?? '—');
  }
  if (next.status !== prev.status) {
    const badge = cell('status')?.querySelector('.badge');
    if (badge) {
      badge.className = `badge ${statusBadgeClass(next.status)}`;
      badge.textContent = next.status;
    }
  }
  if (next.tipoPedido !== prev.tipoPedido) {
    const chip = cell('tipo')?.querySelector('.tipo-chip');
    if (chip) {
      chip.className = `tipo-chip ${tipoChipClass(next.tipoPedido)}`;
      chip.textContent = next.tipoPedido;
    }
  }
  if (next.corTampa !== prev.corTampa) {
    const visual = cell('tampa')?.querySelector('.tampa-visual');
    if (visual) {
      visual.querySelector('.tampa-swatch').style.background = tampaHex(next.corTampa);
      const textNode = [...visual.childNodes].find((n) => n.nodeType === Node.TEXT_NODE);
      if (textNode) textNode.textContent = next.corTampa;
    }
  }
  if (blocosSig(next.blocos ?? []) !== blocosSig(prev.blocos ?? [])) {
    patchInner(cell('blocos')?.querySelector('.blocos-preview'),
      (next.blocos ?? []).map(miniBloco).join(''));
  }
  if (next.dataCriacao !== prev.dataCriacao) {
    patchText(cell('criacao'), formatDateTime(next.dataCriacao));
  }
  if (next.dataEntradaExpedicao !== prev.dataEntradaExpedicao) {
    patchText(cell('expedicao'), formatDateTime(next.dataEntradaExpedicao));
  }
}
