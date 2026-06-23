package com.tecdes.smart.app_smart_40.dto.response;

/**
 * Resposta de {@code POST /api/clp/{estacao}/conectar|desconectar}.
 *
 * @param estacao    nome da estação na API (estoque/processo/montagem/expedicao)
 * @param ip         IP normalizado da estação
 * @param alcancavel se o CLP respondeu ao teste S7 (porta 102)
 * @param leitura    se a leitura read-only da estação está habilitada após a operação
 */
public record ClpConexaoResponseDTO(String estacao, String ip, boolean alcancavel, boolean leitura) {
}
