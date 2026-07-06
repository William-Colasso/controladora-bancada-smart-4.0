// Fila de produção: renderiza a ordem em que os pedidos serão feitos na bancada.
// A numeração (01, 02, …) é a posição real na fila — o head é o que está em produção.
// Render helper puro no padrão components/*: recebe o container e os pedidos já carregados.
import { normalizeStatus, statusBadgeClass } from '../core/enums.js';
import { formatOP, formatCount, tampaHex, formatDuracao } from '../core/format.js';

const STATUS_LABEL = {
  PENDENTE: 'Na fila',
  PRODUCAO: 'Em produção',
  CONCLUIDO: 'Concluído',
};

// Um "stop" da linha de produção. `head` = primeira parada (recebe o pulso quando em produção).
function stopHTML(pedido, index, head) {
  const status = normalizeStatus(pedido.status);
  const ativo = head && status === 'PRODUCAO';
  const timerHTML = ativo && pedido.dataEntradaProducao
    ? `<span class="fila-stop__timer" data-cronometro-start="${pedido.dataEntradaProducao}">00:00:00</span>`
    : status === 'CONCLUIDO' && pedido.dataEntradaProducao && pedido.dataEntradaExpedicao
    ? `<span class="fila-stop__timer fila-stop__timer--done">${formatDuracao(pedido.dataEntradaProducao, pedido.dataEntradaExpedicao)}</span>`
    : '';
  return `
    <li class="fila-stop${ativo ? ' fila-stop--ativo' : ''}" data-pedido-id="${pedido.id}">
      <span class="fila-stop__num">${formatCount(index + 1)}</span>
      <div class="fila-stop__card">
        <div class="fila-stop__op">
          <span class="fila-stop__op-lbl">OP</span>${formatOP(pedido.ordemProducao)}
        </div>
        <div class="fila-stop__meta">
          <span class="fila-stop__id">#${formatCount(pedido.id)}</span>
          <span class="fila-stop__tampa" style="background:${tampaHex(pedido.corTampa)}"></span>
        </div>
        <span class="badge ${statusBadgeClass(status)} fila-stop__badge">${STATUS_LABEL[status] ?? status}</span>
        ${timerHTML}
      </div>
    </li>`;
}

// `filaPedidos`: pedidos na ordem da fila (já cruzados dos ids com os dados carregados).
export function renderFila(container, filaPedidos) {
  if (!container) return;
  const itens = (filaPedidos ?? []).filter(Boolean);

  if (!itens.length) {
    container.innerHTML = `
      <div class="fila-empty">
        <i class="fa-solid fa-inbox"></i>
        <span>Nenhum pedido na fila. Envie um pedido para iniciar a produção.</span>
      </div>`;
    return;
  }

  container.innerHTML = `
    <ol class="fila-track">
      ${itens.map((p, i) => stopHTML(p, i, i === 0)).join('')}
    </ol>`;
}
