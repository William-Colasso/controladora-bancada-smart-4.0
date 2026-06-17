let container;

function ensureContainer() {
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }
}

function show(message, type = 'info', duration = 3500) {
  ensureContainer();
  const icons = { success: '✓', error: '✕', info: 'ℹ' };

  const el = document.createElement('div');
  el.className = `toast toast--${type}`;
  el.innerHTML = `<span style="font-weight:700">${icons[type] || ''}</span> ${message}`;
  container.appendChild(el);

  setTimeout(() => {
    el.style.animation = 'toastIn 200ms ease reverse forwards';
    setTimeout(() => el.remove(), 200);
  }, duration);
}

export const Toast = {
  success: (msg) => show(msg, 'success'),
  error: (msg) => show(msg, 'error'),
  info: (msg) => show(msg, 'info'),
};
