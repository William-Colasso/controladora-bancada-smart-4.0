const COR_BLOCO_CLASS = { PRETO: 'preto', VERMELHO: 'vermelho', AZUL: 'azul', VAZIO: 'vazio' };
const COR_BLOCO_LABEL = { PRETO: 'Preto', VERMELHO: 'Vermelho', AZUL: 'Azul', VAZIO: 'Vazio' };
const STATUS_BADGE = { PENDENTE: 'badge--yellow', PRODUCAO: 'badge--blue', CONCLUIDO: 'badge--green' };
const TIPO_CHIP = { SIMPLES: 'tipo-chip--simples', DUPLO: 'tipo-chip--duplo', TRIPLO: 'tipo-chip--triplo' };

export const COR_INT_TO_NAME = { 0: 'VAZIO', 1: 'PRETO', 2: 'VERMELHO', 3: 'AZUL' };

export const corBlocoClass = (cor) => COR_BLOCO_CLASS[cor] || 'vazio';
export const corBlocoLabel = (cor) => COR_BLOCO_LABEL[cor] || cor;
export const statusBadgeClass = (status) => STATUS_BADGE[status] || 'badge--dim';
export const tipoChipClass = (tipo) => TIPO_CHIP[tipo] || '';
