package com.tecdes.smart.app_smart_40.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.CorTampa;
import com.tecdes.smart.app_smart_40.model.enums.TipoPedido;

public record PedidoRequestDTO(TipoPedido tipoPedido,
        CorTampa corTampa, List<BlocoRequestDTO> blocos) {

    public Pedido toEntity() {
        return Pedido.builder()
                .tipoPedido(this.tipoPedido)
                .corTampa(this.corTampa)
                .dataCriacao(LocalDateTime.now())
                .blocos(this.blocos.stream().map(BlocoRequestDTO::toEntity).toList())
                .build();
    }
}
