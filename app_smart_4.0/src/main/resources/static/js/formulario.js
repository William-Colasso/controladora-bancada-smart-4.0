document.addEventListener("DOMContentLoaded", () => {
  "use strict";

  // ── Helpers ────────────────────────────────────────
  function buildSel(data, cls) {
    const el = document.createElement("select");
    el.className = cls;
    data.forEach(({ nome, valor }) => {
      const opt = document.createElement("option");
      opt.value = valor;
      opt.textContent = nome;
      el.appendChild(opt);
    });
    return el;
  }

  function syncAddBtn(btn, count) {
    btn.disabled = count >= 3;
    btn.textContent = `+ LÂMINA (${count}/3)`;
  }

  // ── tipoPedido → ativar/desativar blocos ──────────
  const tipoPedidoEl = document.getElementById("tipo-pedido");

  function syncBlocos() {
    const qty = parseInt(tipoPedidoEl.value, 10);
    ["bloco-1", "bloco-2", "bloco-3"].forEach((id, idx) => {
      const card = document.getElementById(id);
      const active = idx < qty;
      card.classList.toggle("inactive", !active);
      if (!active) resetBloco(card); // limpa lâminas ao desativar
    });
  }

  function resetBloco(card) {
    const list = card.querySelector(".laminas-list");
    list.innerHTML = '<div class="empty-lam">Nenhuma lâmina adicionada.</div>';
    syncAddBtn(card.querySelector(".btn-add-lam"), 0);
  }

  tipoPedidoEl.addEventListener("change", syncBlocos);
  syncBlocos(); // aplica no carregamento da página

  // ── Adicionar Lâmina ──────────────────────────────
  function addLamina(btn) {
    const body = btn.closest(".bloco-body");
    const list = body.querySelector(".laminas-list");
    const count = list.querySelectorAll(".lamina-row").length;
    if (count >= 3) return;

    // remove placeholder
    const ph = list.querySelector(".empty-lam");
    if (ph) ph.remove();

    // cria linha
    const row = document.createElement("div");
    row.className = "lamina-row";

    row.appendChild(buildSel(CORES_LAMINAS, "lam-sel lam-cor"));
    row.appendChild(buildSel(PADROES_LAMINA, "lam-sel lam-pad"));
    row.appendChild(buildSel(POSICOES_LAMINA, "lam-sel lam-pos"));

    const rmBtn = document.createElement("button");
    rmBtn.className = "btn-rm-lam";
    rmBtn.textContent = "×";
    rmBtn.onclick = () => {
      row.remove();
      const remaining = list.querySelectorAll(".lamina-row").length;
      if (!remaining)
        list.innerHTML =
          '<div class="empty-lam">Nenhuma lâmina adicionada.</div>';
      syncAddBtn(btn, remaining);
    };
    row.appendChild(rmBtn);
    list.appendChild(row);

    syncAddBtn(btn, count + 1);
  }

  // ── Montar Payload ────────────────────────────────
  function buildPayload() {
    const blocos = [];

    ["bloco-1", "bloco-2", "bloco-3"].forEach((id) => {
      const card = document.getElementById(id);
      if (card.classList.contains("inactive")) return;

      const cor = parseInt(card.querySelector(".cor-bloco-sel").value, 10);
      const laminas = [];

      card.querySelectorAll(".lamina-row").forEach((row) => {
        laminas.push({
          cor: parseInt(row.querySelector(".lam-cor").value, 10),
          padrao: parseInt(row.querySelector(".lam-pad").value, 10),
          posicao: parseInt(row.querySelector(".lam-pos").value, 10),
        });
      });

      blocos.push({ cor, laminas });
    });

    return {
      tipoPedido: parseInt(tipoPedidoEl.value, 10),
      corTampa: parseInt(document.getElementById("cor-tampa").value, 10),
      blocos,
    };
  }

  // ── Enviar ────────────────────────────────────────
  async function enviarPedido() {
    const btn = document.getElementById("btn-submit");
    const panel = document.getElementById("resp-panel");
    const titleEl = document.getElementById("resp-title");
    const bodyEl = document.getElementById("resp-body");

    btn.disabled = true;
    btn.classList.add("loading");
    btn.textContent = "PROCESSANDO...";

    const payload = buildPayload();

    try {
      const res = await fetch("/api/pedidos", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload, null, 2),
      });

      let data;
      try {
        data = await res.json();
      } catch {
        data = await res.text();
      }

      panel.style.display = "block";

      if (res.ok) {
        panel.className = "ok";
        titleEl.style.color = "var(--success)";
        titleEl.textContent = `✓  PEDIDO CRIADO  ·  HTTP ${res.status}`;
      } else {
        panel.className = "err";
        titleEl.style.color = "var(--danger)";
        titleEl.textContent = `✕  ERRO  ·  HTTP ${res.status}`;
      }

      bodyEl.textContent =
        typeof data === "string" ? data : JSON.stringify(data, null, 2);

      panel.scrollIntoView({ behavior: "smooth", block: "nearest" });
    } catch (err) {
      panel.style.display = "block";
      panel.className = "err";
      titleEl.style.color = "var(--danger)";
      titleEl.textContent = "✕  FALHA NA CONEXÃO";
      bodyEl.textContent = err.message;
    } finally {
      btn.disabled = false;
      btn.classList.remove("loading");
      btn.textContent = "ENVIAR PEDIDO";
    }
  }

  // ── Limpar ────────────────────────────────────────
  function limparForm() {
    tipoPedidoEl.selectedIndex = 0;
    document.getElementById("cor-tampa").selectedIndex = 0;

    ["bloco-1", "bloco-2", "bloco-3"].forEach((id) => {
      const card = document.getElementById(id);
      card.querySelector(".cor-bloco-sel").selectedIndex = 0;
      resetBloco(card);
    });

    const panel = document.getElementById("resp-panel");
    panel.style.display = "none";
    panel.className = "";

    syncBlocos();
  }

  window.limparForm = limparForm;
  window.enviarPedido = enviarPedido;
  window.addLamina = addLamina;
});
