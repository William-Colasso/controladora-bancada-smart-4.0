const COR_BLOCO_CLASS = {
  PRETO: "preto",
  VERMELHO: "vermelho",
  AZUL: "azul",
  VAZIO: "vazio",
};
const COR_BLOCO_LABEL = {
  PRETO: "Preto",
  VERMELHO: "Vermelho",
  AZUL: "Azul",
  VAZIO: "Vazio",
};
const STATUS_BADGE = {
  PENDENTE: "badge--yellow",
  PRODUCAO: "badge--blue",
  CONCLUIDO: "badge--green",
};
const TIPO_CHIP = {
  SIMPLES: "tipo-chip--simples",
  DUPLO: "tipo-chip--duplo",
  TRIPLO: "tipo-chip--triplo",
};

export const COR_INT_TO_NAME = {
  0: "VAZIO",
  1: "PRETO",
  2: "VERMELHO",
  3: "AZUL",
};

export const corBlocoClass = (cor) => COR_BLOCO_CLASS[cor] || "vazio";
export const corBlocoLabel = (cor) => COR_BLOCO_LABEL[cor] || cor;
export const statusBadgeClass = (status) =>
  STATUS_BADGE[status] || "badge--dim";
export const tipoChipClass = (tipo) => TIPO_CHIP[tipo] || "";

export const STATUS_INT_TO_NAME = {
  1: "PENDENTE",
  2: "PRODUCAO",
  3: "CONCLUIDO",
};
export const TIPO_INT_TO_NAME = { 1: "SIMPLES", 2: "DUPLO", 3: "TRIPLO" };
export const PADRAO_INT_TO_NAME = {
  0: "NENHUM",
  1: "CASA",
  2: "NAVIO",
  3: "ESTRELA",
};
export const POSICAO_INT_TO_NAME = { 0: "ESQUERDA", 1: "FRENTE", 2: "DIREITA" };
export const COR_LAMINA_INT_TO_NAME = {
    0:"NENHUM",
  1: "VERMELHO",
  2: "AZUL",
  3: "AMARELO",
  4: "VERDE",
  5: "PRETO",
  6: "BRANCO",
};

export const normalizeStatus = (v) => STATUS_INT_TO_NAME[v] ?? v;
export const normalizeTipo = (v) => TIPO_INT_TO_NAME[v] ?? v;
export const normalizeCor = (v) => COR_LAMINA_INT_TO_NAME[v] ?? v;
export const normalizeCorTampa = (v) => COR_INT_TO_NAME ?? v;
export const normalizeCorBloco = (v) => COR_INT_TO_NAME[v] ?? v;
export const normalizePadrao = (v) => PADRAO_INT_TO_NAME[v] ?? v;
export const normalizePosicao = (v) => POSICAO_INT_TO_NAME[v] ?? v;
