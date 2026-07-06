// Banner global "SEM COMUNICAÇÃO COM O CLP" — evidencia a perda de leitura em qualquer tela SSE.
// Liveness = pulsos `estacao-heartbeat` (write path emite um por passada lida). Sem pulso por
// mais que `limiteMs` (ou nenhum pulso após `graceMs` do load) → banner visível +
// body[data-comunicacao="off"] (hook de CSS para estados degradados por página).
//
// Uso (antes de sse.connect()):
//   import { initCommBanner } from '../components/commBanner.js';
//   initCommBanner(sse);
export function initCommBanner(sse, { graceMs = 4000, limiteMs = 10000 } = {}) {
  const el = document.createElement('div');
  el.className = 'comm-banner';
  el.hidden = true;
  el.setAttribute('role', 'alert');
  el.innerHTML = `
    <i class="fa-solid fa-triangle-exclamation"></i>
    <span class="comm-banner__msg">Sem comunicação com o CLP</span>
    <span class="comm-banner__hint">verifique os IPs em <a href="/configuracao">Configuração</a> e a rede da bancada</span>`;
  document.body.prepend(el);

  let ultimoPulso = 0;
  const inicio = Date.now();
  sse.on('estacao-heartbeat', () => { ultimoPulso = Date.now(); });
  //sse.on('estacao-heartbeat', ()=> console.log("AAA"))
  const intervalId = setInterval(() => {
    const agora = Date.now();
    const sem = ultimoPulso === 0
      ? (agora - inicio) > graceMs      // nunca pulsou desde o load
      : (agora - ultimoPulso) > limiteMs; // pulsava e parou
    el.hidden = !sem;
    document.body.dataset.comunicacao = sem ? 'off' : 'on';
  }, 1000);

  return () => {
    clearInterval(intervalId);
    sse.off?.('estacao-heartbeat', onHeartbeat); // se sse.on tiver um `off` correspondente
    el.remove();
  };
}
