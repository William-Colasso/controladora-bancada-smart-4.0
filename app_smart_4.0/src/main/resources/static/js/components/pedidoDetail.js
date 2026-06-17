import { corBlocoClass, corBlocoLabel, statusBadgeClass, tipoChipClass } from '../core/enums.js';
import { formatCount, formatDateTime, tampaHex } from '../core/format.js';
import { patchText, patchInner } from '../core/dom.js';

const blocosSig = (blocos) => JSON.stringify(blocos);

function laminaHTML(l) {
  return `
    <div class="lamina-row">
      <div class="lamina-swatch lamina-swatch--${l.cor}"></div>
      <span class="lamina-cor">${l.cor}</span>
      <span class="lamina-padrao">${l.padrao !== 'NENHUM' ? l.padrao : ''}</span>
      <span class="lamina-pos">${posicaoLabel(l.posicaoNoBloco)}</span>
    </div>`;
}

const POSICAO_LABEL = { ESQUERDA: '← Esq', FRENTE: '↑ Frente', DIREITA: '→ Dir' };
const posicaoLabel = (pos) => POSICAO_LABEL[pos] ?? pos;

function blocoDetailHTML(b, i) {
  const laminas = b.laminas ?? [];
  const posEl = b.estoque ? `<span class="bloco-detail-card__pos">Pos. ${b.estoque.posicao}</span>` : '';
  const laminasH = laminas.length > 0 ? laminas.map(laminaHTML).join('') : '<p class="no-laminas">Sem lâminas</p>';
  return `
    <div class="bloco-detail-card" data-bloco-idx="${i}">
      <div class="bloco-detail-card__header">
        <div class="bloco-color-bar bloco-color-bar--${corBlocoClass(b.cor)}"></div>
        <span class="bloco-detail-card__name">Bloco ${i + 1} — ${corBlocoLabel(b.cor)}</span>
        ${posEl}
      </div>
      <div class="laminas-list">${laminasH}</div>
    </div>`;
}

function infoItemHTML(key, val, field = '', style = '') {
  const fieldAttr = field ? ` data-field="${field}"` : '';
  const styleAttr = style ? ` style="${style}"` : '';
  return `
    <div class="info-item">
      <div class="info-item__key">${key}</div>
      <div class="info-item__val"${fieldAttr}${styleAttr}>${val}</div>
    </div>`;
}

export function buildDetailHTML(p) {
  const blocos = p.blocos ?? [];
  return `
    <div class="info-grid">
      ${infoItemHTML('Pedido', `#${formatCount(p.id)}`)}
      ${infoItemHTML('Ordem Produção', p.ordemProducao ?? '—', 'op')}
      ${infoItemHTML('Status', `<span class="badge ${statusBadgeClass(p.status)}">${p.status}</span>`, 'status')}
      ${infoItemHTML('Tipo', `<span class="tipo-chip ${tipoChipClass(p.tipoPedido)}">${p.tipoPedido}</span>`, 'tipo')}
      ${infoItemHTML('Cor da Tampa',
        `<div class="tampa-visual">
           <div class="tampa-swatch" style="background:${tampaHex(p.corTampa)}"></div>
           ${p.corTampa}
         </div>`, 'tampa')}
      ${infoItemHTML('Criado em', formatDateTime(p.dataCriacao), 'criacao', 'font-size:12px')}
      ${infoItemHTML('Entrada Expedição', formatDateTime(p.dataEntradaExpedicao), 'expedicao', 'font-size:12px')}
      ${infoItemHTML('Blocos', String(blocos.length), 'blocos-count')}
    </div>
    <div class="blocos-detail-title">BLOCOS & LÂMINAS</div>
    <div class="blocos-detail-list" data-field="blocos-list">
      ${blocos.length > 0 ? blocos.map(blocoDetailHTML).join('') : '<p class="no-laminas">Nenhum bloco neste pedido.</p>'}
    </div>`;
}

export function patchDetail(detailBody, next, prev) {
  const field = (name) => detailBody.querySelector(`[data-field="${name}"]`);

  if (next.ordemProducao !== prev.ordemProducao) {
    patchText(field('op'), next.ordemProducao ?? '—');
  }
  if (next.status !== prev.status) {
    const badge = field('status')?.querySelector('.badge');
    if (badge) {
      badge.className = `badge ${statusBadgeClass(next.status)}`;
      badge.textContent = next.status;
    }
  }
  if (next.tipoPedido !== prev.tipoPedido) {
    const chip = field('tipo')?.querySelector('.tipo-chip');
    if (chip) {
      chip.className = `tipo-chip ${tipoChipClass(next.tipoPedido)}`;
      chip.textContent = next.tipoPedido;
    }
  }
  if (next.corTampa !== prev.corTampa) {
    const container = field('tampa');
    if (container) {
      container.querySelector('.tampa-swatch').style.background = tampaHex(next.corTampa);
      const visual = container.querySelector('.tampa-visual');
      const textNode = [...visual.childNodes].find((n) => n.nodeType === Node.TEXT_NODE);
      if (textNode) textNode.textContent = next.corTampa;
    }
  }
  if (next.dataCriacao !== prev.dataCriacao) {
    patchText(field('criacao'), formatDateTime(next.dataCriacao));
  }
  if (next.dataEntradaExpedicao !== prev.dataEntradaExpedicao) {
    patchText(field('expedicao'), formatDateTime(next.dataEntradaExpedicao));
  }
  if (blocosSig(next.blocos ?? []) !== blocosSig(prev.blocos ?? [])) {
    const blocos = next.blocos ?? [];
    patchText(field('blocos-count'), String(blocos.length));
    patchInner(field('blocos-list'),
      blocos.length > 0 ? blocos.map(blocoDetailHTML).join('') : '<p class="no-laminas">Nenhum bloco neste pedido.</p>');
  }
}
