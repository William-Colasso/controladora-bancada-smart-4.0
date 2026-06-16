const TAMPA_HEX = { PRETO: '#333', VERMELHO: '#fc0518', AZUL: '#2b92d5' };

export const formatDateTime = (iso) => {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
};

export const formatCount = (n) => String(n).padStart(3, '0');

export const formatOP = (op) => (op == null ? '—' : String(op).padStart(3, '0'));

export const tampaHex = (cor) => TAMPA_HEX[cor] ?? '#555';

export const debounce = (fn, ms) => {
  let t;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), ms);
  };
};
