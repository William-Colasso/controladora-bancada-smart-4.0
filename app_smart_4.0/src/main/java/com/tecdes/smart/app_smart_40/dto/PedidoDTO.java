package com.tecdes.smart.app_smart_40.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.CorTampa;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.model.enums.TipoPedido;

public record PedidoDTO(
    Long id,
    Integer ordemProducao,
    StatusPedido status,
    TipoPedido tipoPedido,
    CorTampa corTampa,
    LocalDateTime dataCriacao,
    LocalDateTime dataEntradaExpedicao,
    List<Bloco> blocos,
    Expedicao expedicao
) {
    public static PedidoDTO fromEntity(Pedido pedido) {
        return new PedidoDTO(
            pedido.getId(),
            pedido.getOrdemProducao(),
            pedido.getStatus(),
            pedido.getTipoPedido(),
            pedido.getCorTampa(),
            pedido.getDataCriacao(),
            pedido.getDataEntradaExpedicao(),
            pedido.getBlocos(),
            pedido.getExpedicao()
        );
    }

    public Pedido toEntity() {
        return Pedido.builder()
            .id(this.id)
            .ordemProducao(this.ordemProducao)
            .status(this.status)
            .tipoPedido(this.tipoPedido)
            .corTampa(this.corTampa)
            .dataCriacao(this.dataCriacao != null ? this.dataCriacao : LocalDateTime.now())
            .dataEntradaExpedicao(this.dataEntradaExpedicao)
            .blocos(this.blocos)
            .expedicao(this.expedicao)
            .build();
    }
}