const TAMPA_HEX = {
  PRETO:    '#333',
  VERMELHO: '#fc0518',
  AZUL:     '#2b92d5',
  1: '#333',
  2: '#fc0518',
  3: '#2b92d5',
};

export const formatDateTime = (iso) => {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
};

export const formatCount = (n) => String(n).padStart(3, '0');

export const formatOP = (op) => (op == null ? '—' : String(op).padStart(3, '0'));

export const tampaHex = (cor) => TAMPA_HEX[cor] ?? '#555';

export const formatDuracao = (start, end = null) => {
  if (!start) return '—';
  const ms = (end ? new Date(end) : new Date()) - new Date(start);
  if (ms < 0) return '00:00:00';
  const s = Math.floor(ms / 1000), m = Math.floor(s / 60), h = Math.floor(m / 60);
  return `${String(h).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
};

export const debounce = (fn, ms) => {
  let t;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), ms);
  };
};


