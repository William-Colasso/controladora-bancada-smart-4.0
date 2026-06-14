/**
 * home.js
 * Lógica da tela inicial — conexão com o CLP via IP informado pelo usuário.
 */

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

  // TODO(human): chamar POST /api/pedidos/clp/{ip} e, de acordo com o
  // resultado, chamar setClpBadge('green', 'OK', 'fa-circle-check') em caso
  // de sucesso ou setClpBadge('red', 'Erro', 'fa-circle-xmark') em caso de
  // falha (resposta não-OK ou exceção de rede). Lembre-se de usar
  // encodeURIComponent(ip) na URL e de reabilitar o botão (btn.disabled =
  // false) ao final, mesmo em caso de erro.
}
