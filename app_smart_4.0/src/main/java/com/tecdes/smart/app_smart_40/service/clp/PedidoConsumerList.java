package com.tecdes.smart.app_smart_40.service.clp;

import com.tecdes.smart.app_smart_40.dto.response.PedidoResponseDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.clp.ExpedicaoCLP;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.service.PedidoService;
import com.tecdes.smart.app_smart_40.service.SmartService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoConsumerList {

    public static ConcurrentLinkedQueue<Long> pedidos = new ConcurrentLinkedQueue<>();
    private final SmartService smartService;
    private final ExpedicaoCLP expedicaoCLP;
    private final ExpedicaoClpWriter expedicaoClpWriter;
    private final PedidoService pedidoService;
    private Pedido current = null;

    @Scheduled(fixedDelayString = "${delay.order.queue:1000}")
    @Transactional
    public void processOrder() {
        System.out.println("Lista de pedidos:       " + pedidos.toString());
        // Nada na fila — nada a fazer
        if (pedidos.peek() == null)
            return;

        // Carrega o current se ainda não tem
        if (current == null) {
            current = pedidoService.buscarPorId(pedidos.peek()).toEntity();
            smartService.enviarParaProducao(current.getId());
            return; // aguarda próxima execução para checar conclusão
        }

        // Verifica se o pedido em produção foi concluído
        boolean concluido = current.getOrdemProducao()
                .equals(expedicaoCLP.getNumeroOP());

        if (concluido) {

            try {
                expedicaoClpWriter.escreverPosicao(current.getExpedicao().getPosicao(),
                        current.getOrdemProducao().intValue());

                PedidoResponseDTO p = pedidoService.concluir(current.getId());
                if (p.status().equals(StatusPedido.CONCLUIDO)) {
                    pedidos.poll(); // remove o head da fila de forma segura
                    current = null; // libera para o próximo pedido
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }

    }

    public void addOrder(Long id) {
        pedidos.add(id);
    }

}
