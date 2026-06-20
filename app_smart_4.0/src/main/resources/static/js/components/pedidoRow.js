import { corBlocoClass, statusBadgeClass, tipoChipClass, normalizeStatus, normalizeTipo, normalizeCor, normalizeCorBloco } from '../core/enums.js';
import { formatCount, formatDateTime, tampaHex } from '../core/format.js';
import { patchText, patchInner } from '../core/dom.js';

const blocosSig = (blocos) => JSON.stringify(blocos);
const miniBloco = (b) => `<div class="mini-bloco mini-bloco--${corBlocoClass(normalizeCorBloco(b.cor))}"></div>`;

export function buildRowHTML(p) {
  const status   = normalizeStatus(p.status);
  const tipo     = normalizeTipo(p.tipoPedido);
  const corTampa = normalizeCor(p.corTampa);
  const blocos   = p.blocos ?? [];
  return `
    <td data-cell="id">#${formatCount(p.id)}</td>
    <td data-cell="op">${p.ordemProducao ?? '—'}</td>
    <td data-cell="status">
      <span class="badge ${statusBadgeClass(status)}">${status}</span>
    </td>
    <td data-cell="tipo">
      <span class="tipo-chip ${tipoChipClass(tipo)}">${tipo}</span>
    </td>
    <td data-cell="tampa">
      <div class="tampa-visual">
        <div class="tampa-swatch" style="background:${tampaHex(p.corTampa)}"></div>
        ${corTampa}
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
      const status = normalizeStatus(next.status);
      badge.className = `badge ${statusBadgeClass(status)}`;
      badge.textContent = status;
    }
  }
  if (next.tipoPedido !== prev.tipoPedido) {
    const chip = cell('tipo')?.querySelector('.tipo-chip');
    if (chip) {
      const tipo = normalizeTipo(next.tipoPedido);
      chip.className = `tipo-chip ${tipoChipClass(tipo)}`;
      chip.textContent = tipo;
    }
  }
  if (next.corTampa !== prev.corTampa) {
    const visual = cell('tampa')?.querySelector('.tampa-visual');
    if (visual) {
      visual.querySelector('.tampa-swatch').style.background = tampaHex(next.corTampa);
      const textNode = [...visual.childNodes].find((n) => n.nodeType === Node.TEXT_NODE);
      if (textNode) textNode.textContent = normalizeCor(next.corTampa);
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
