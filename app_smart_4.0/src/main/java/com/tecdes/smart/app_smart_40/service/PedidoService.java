package com.tecdes.smart.app_smart_40.service;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.dto.PedidoDTO;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    private Boolean validarTipoPedido(PedidoDTO pedido) {
        return pedido.blocos().size() == pedido.tipoPedido().getValue();
    }

    

}
