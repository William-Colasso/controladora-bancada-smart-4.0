import { Toast } from '../core/toast.js';

function setClpBadge(status, label, icon) {
  const badge = document.getElementById('clp-status-badge');
  badge.className = `badge badge--${status}`;
  badge.innerHTML = `<i class="fa-solid ${icon}"></i> ${label}`;
}

async function conectarClp() {
  const input = document.getElementById('ip-clp');
  const btn = document.getElementById('btn-conectar-clp');
  const ip = input.value.trim();

  if (!ip) {
    Toast.error('Informe o IP do CLP.');
    return;
  }

  btn.disabled = true;
  setClpBadge('dim', 'Conectando...', 'fa-circle-notch fa-spin');

  // TODO(human): POST /api/pedidos/clp/{encodeURIComponent(ip)}; em sucesso
  // setClpBadge('green','OK','fa-circle-check'), em falha 'red'/'fa-circle-xmark';
  // reabilitar btn ao final.
}

document.getElementById('btn-conectar-clp').addEventListener('click', conectarClp);
