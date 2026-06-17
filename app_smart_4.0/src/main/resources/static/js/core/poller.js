export function createPoller(fetchFn, intervalMs, onData) {
  let timer = null;
  let paused = false;

  async function run() {
    try {
      onData(await fetchFn());
    } catch (err) {
      console.error('[Poller] erro no fetch:', err);
    }
  }

  return {
    start() {
      run();
      timer = setInterval(() => { if (!paused) run(); }, intervalMs);
    },
    stop() {
      if (timer) clearInterval(timer);
      timer = null;
    },
    pause() { paused = true; },
    resume() { paused = false; },
    refresh: run,
  };
}
