const MAX_LAMINAS = 3;

function buildSelect(data, cls) {
  const sel = document.createElement('select');
  sel.className = cls;
  data.forEach(({ nome, valor }) => {
    const opt = document.createElement('option');
    opt.value = valor;
    opt.textContent = nome;
    sel.appendChild(opt);
  });
  return sel;
}

export function emptyPlaceholder() {
  return '<div class="empty-lam">Nenhuma lâmina adicionada.</div>';
}

export function syncAddButton(btn, count) {
  btn.disabled = count >= MAX_LAMINAS;
  btn.textContent = `+ LÂMINA (${count}/${MAX_LAMINAS})`;
}

export function createLaminaRow(opcoes, onRemove) {
  const row = document.createElement('div');
  row.className = 'lamina-row';

  row.appendChild(buildSelect(opcoes.cores, 'lam-sel lam-cor'));
  row.appendChild(buildSelect(opcoes.padroes, 'lam-sel lam-pad'));
  row.appendChild(buildSelect(opcoes.posicoes, 'lam-sel lam-pos'));

  const rmBtn = document.createElement('button');
  rmBtn.className = 'btn-rm-lam';
  rmBtn.textContent = '×';
  rmBtn.onclick = () => onRemove(row);
  row.appendChild(rmBtn);

  return row;
}

export { MAX_LAMINAS };
