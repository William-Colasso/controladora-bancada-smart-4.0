package com.tecdes.smart.app_smart_40.model.enums;

/**
 * As quatro estações físicas da bancada, cada uma num CLP com IP próprio.
 *
 * <p>{@code frontKey} é a chave usada no frontend ({@code bancadaStatus.js#ESTACOES} e nos eventos SSE)
 * — note que PROCESSO mapeia para {@code "producao"}. {@link #apiName()} é o identificador usado na
 * API REST de configuração de IP ({@code /api/clp/ips/{estacao}}).
 */
public enum EstacaoClp {

    ESTOQUE("estoque"),
    PROCESSO("producao"),
    MONTAGEM("montagem"),
    EXPEDICAO("expedicao");

    private final String frontKey;

    EstacaoClp(String frontKey) {
        this.frontKey = frontKey;
    }

    /** Chave da estação no frontend/eventos SSE (estoque/producao/montagem/expedicao). */
    public String getFrontKey() {
        return frontKey;
    }

    /** Identificador da estação na API REST (estoque/processo/montagem/expedicao). */
    public String apiName() {
        return name().toLowerCase();
    }

    /** Resolve a estação a partir do path da API; lança 400 (IllegalArgument) se inválida. */
    public static EstacaoClp fromApi(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Estação é obrigatória.");
        }
        try {
            return EstacaoClp.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estação inválida: " + valor + ". Use: estoque, processo, montagem, expedicao.");
        }
    }
}
