// Wrapper sobre EventSource: assina eventos SSE nomeados, com reconexão automática (backoff).
// Espelha o estilo dos singletons de core/ (ex.: createPoller): factory, sem classes.
//
// Uso:
//   import { createSse } from '../core/sse.js';
//   const sse = createSse();              // default: '/api/stream'
//   sse.on('estoque', (d) => { ... });    // d = payload JSON já parseado
//   sse.connect();
export function createSse(path = '/api/stream') {
  let source = null;
  let stopped = false; // close() explícito: não religar em erro nem no bfcache
  const handlers = new Map(); // nomeEvento → Set(callback)
  let backoff = 1000;

  function bind(evento) {
    source.addEventListener(evento, (e) => {
      let data;
      try { data = JSON.parse(e.data); } catch (_) { data = e.data; }
      handlers.get(evento)?.forEach((cb) => {
        try { cb(data); } catch (err) { console.error(`[SSE] handler '${evento}':`, err); }
      });
    });
  }

  function connect() {
    source = new EventSource(path);
    source.onopen = () => { backoff = 1000; };
    handlers.forEach((_set, evento) => bind(evento)); // (re)liga todos os eventos já registrados
    source.onerror = () => {
      source.close();
      if (stopped) return;
      setTimeout(connect, backoff);
      backoff = Math.min(backoff * 2, 15000);
    };
  }

  // Libera o socket SSE ao sair da página. Sem isto, cada navegação deixa o EventSource
  // aberto (o servidor só expira em 1h) e o navegador esgota o limite ~6 conexões/host em
  // HTTP/1.1 → as próximas páginas ficam presas esperando um slot livre. pagehide cobre a
  // navegação normal; se a página voltar do bfcache, pageshow(persisted) religa.
  window.addEventListener('pagehide', () => { if (source) source.close(); });
  window.addEventListener('pageshow', (e) => { if (e.persisted && !stopped) connect(); });

  return {
    on(evento, cb) {
      if (!handlers.has(evento)) handlers.set(evento, new Set());
      handlers.get(evento).add(cb);
      if (source && source.readyState !== EventSource.CLOSED) bind(evento); // já conectado → liga agora
      return this;
    },
    connect,
    close() { stopped = true; if (source) source.close(); },
  };
}
