package com.tecdes.smart.app_smart_40.service.clp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.clp.ExpedicaoCLP;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import com.tecdes.smart.app_smart_40.service.PedidoService;
import com.tecdes.smart.app_smart_40.service.SmartService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fila de pedidos: serializa a produção em "um pedido por vez" na bancada.
 *
 * <p>O estado autoritativo de "o que está rodando" é o <b>banco</b>
 * ({@code status == PRODUCAO}), não a fila em memória — assim um restart com um
 * pedido em curso não faz a fila sobrepor outro na bancada. A fila apenas
 * decide qual pedido PENDENTE enviar em seguida.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoConsumerList {

    public static ConcurrentLinkedQueue<Long> pedidos = new ConcurrentLinkedQueue<>();

    private final SmartService smartService;
    private final ExpedicaoCLP expedicaoCLP;
    private final ExpedicaoClpWriter expedicaoClpWriter;
    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;

    @Scheduled(fixedDelayString = "${delay.order.queue:1000}")
    @Transactional
    public void processOrder() {
        log.debug("Fila de pedidos: {}", pedidos);

        // 1) Bancada ocupada? O banco é a fonte da verdade — inclui um pedido
        //    em PRODUCAO órfão de reset que já não está na fila. Enquanto houver
        //    um rodando, NUNCA enviamos outro.
        Optional<Pedido> emProducao = pedidoRepository.findFirstByStatus(StatusPedido.PRODUCAO);
        if (emProducao.isPresent()) {
            tratarEmProducao(emProducao.get());
            return;
        }

        // 2) Nada rodando — avalia o head da fila.
        Long head = pedidos.peek();
        if (head == null) {
            return;
        }

        Pedido pedido = pedidoRepository.findById(head).orElse(null);
        if (pedido == null) {
            // Pedido enfileirado foi deletado: descarta o id para não travar a fila.
            log.warn("Pedido {} não existe mais — removido da fila.", head);
            pedidos.poll();
            return;
        }

        switch (pedido.getStatus()) {
            case CONCLUIDO -> pedidos.poll();                       // já terminou → avança
            case PENDENTE -> smartService.enviarParaProducao(head); // dispara → vira PRODUCAO
            default -> { /* PRODUCAO é tratado no passo 1 */ }
        }
    }

    /**
     * Pedido em produção: se o CLP de expedição já reporta a OP, grava posição+OP no
     * magazine e conclui (idempotente). Avança a fila se este for o head.
     */
    private void tratarEmProducao(Pedido pedido) {
        boolean concluido = pedido.getOrdemProducao().equals(expedicaoCLP.getNumeroOP());
        if (!concluido) {
            return; // ainda executando — aguarda
        }

        try {
            int posicao = pedido.getExpedicao().getPosicao().intValue();
            int op = pedido.getOrdemProducao().intValue();
            log.debug("Guardando OP {} na posição de expedição {}", op, posicao);
            expedicaoClpWriter.escreverPosicao(posicao, op);

            // O auto-sync (ExpedicaoService.guardarNaPosicao) pode ter concluído antes;
            // a checagem evita o IllegalStateException de concluir() e o try blinda a corrida.
            if (pedido.getStatus() != StatusPedido.CONCLUIDO) {
                pedidoService.concluir(pedido.getId());
            }

            Long head = pedidos.peek();
            if (head != null && head.equals(pedido.getId())) {
                pedidos.poll();
            }
        } catch (Exception e) {
            log.error("Falha ao concluir pedido {}: {}", pedido.getId(), e.getMessage());
        }
    }

    public void addOrder(Long id) {
        pedidos.add(id);
    }

    /** Snapshot ordenado dos ids na fila (head = em produção). Para exibição no frontend. */
    public List<Long> filaAtual() {
        return new ArrayList<>(pedidos);
    }

}
