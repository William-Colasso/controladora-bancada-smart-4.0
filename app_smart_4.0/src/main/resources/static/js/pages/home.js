import { Toast } from '../core/toast.js';

function setClpBadge(status, label, icon) {
  const badge = document.getElementById('clp-status-badge');
  badge.className = `badge badge--${status}`;
  badge.innerHTML = `<i class="fa-solid ${icon}"></i> ${label}`;
}

async function conectarClp(button) {

  
  const divpai = button.parentElement;
  const input = divpai.querySelector('input.ip-clp');
  const btn = button;
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

document.querySelectorAll('.btn-conectar-clp').forEach((button) => {
  button.addEventListener('click', () => conectarClp(button));
})
