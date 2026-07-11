package com.tecdes.smart.app_smart_40.service.clp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.clp.ExpedicaoCLP;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import com.tecdes.smart.app_smart_40.service.PedidoService;
import com.tecdes.smart.app_smart_40.service.SmartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fila de pedidos: serializa a produção em "um pedido por vez" na bancada.
 *
 * <p>
 * O estado autoritativo de "o que está rodando" é o <b>banco</b>
 * ({@code status == PRODUCAO}), não a fila em memória — assim um restart com um
 * pedido em curso não faz a fila sobrepor outro na bancada. A fila apenas
 * decide qual pedido PENDENTE enviar em seguida.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoConsumerList {

    private final ConcurrentLinkedQueue<Long> pedidos = new ConcurrentLinkedQueue<>();

    private final SmartService smartService;
    private final ExpedicaoCLP expedicaoCLP;
    private final ExpedicaoClpWriter expedicaoClpWriter;
    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;

    // Sem @Transactional: a comunicação com o CLP (Thread.sleep de 800ms em
    // enviarParaProducao,
    // socket em escreverPosicao) não pode rodar segurando uma conexão JDBC. Cada
    // passo que toca o
    // banco é transacional por conta própria — findFirst/findById (repo readOnly),
    // concluir()
    // (@Transactional) e o persist de enviarParaProducao (TransactionTemplate).
    // Pedido.expedicao é
    // @ManyToOne EAGER, então tratarEmProducao lê a posição sem sessão aberta; o
    // fluxo é idempotente.
    @Scheduled(fixedDelayString = "${delay.order.queue:1000}")
    public void processOrder() {
        log.debug("Fila de pedidos: {}", pedidos);

        // 1) Bancada ocupada? O banco é a fonte da verdade — inclui um pedido
        // em PRODUCAO órfão de reset que já não está na fila. Enquanto houver
        // um rodando, NUNCA enviamos outro.
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
            case CONCLUIDO -> pedidos.poll(); // já terminou → avança
            case PENDENTE -> smartService.enviarParaProducao(head); // dispara → vira PRODUCAO
            default -> {
                /* PRODUCAO é tratado no passo 1 */ }
        }
    }

    /**
     * Pedido em produção: se o CLP de expedição confirma a peça guardada, grava
     * posição+OP no
     * magazine e conclui (idempotente). Avança a fila se este for o head.
     */
    private void tratarEmProducao(Pedido pedido) {
        if (pedido.getExpedicao() == null) {
            return; // órfão sem reserva de expedição — nada a reconciliar
        }
        int posicao = pedido.getExpedicao().getPosicao().intValue();
        int op = pedido.getOrdemProducao();
        if (!pecaGuardada(posicao, op)) {
            return; // ainda executando — aguarda
        }

        try {
            log.debug("Guardando OP {} na posição de expedição {}", op, posicao);
            expedicaoClpWriter.escreverPosicao(posicao, op);

            // O auto-sync (ExpedicaoService.guardarNaPosicao) pode ter concluído antes;
            // a checagem evita o IllegalStateException de concluir() e o try blinda a
            // corrida.
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

    /**
     * Peça guardada = o magazine do CLP na posição reservada mostra a OP (valor de
     * nível, relido
     * a cada ciclo e retido no DB do CLP → sobrevive a restart), OU o CLP ainda
     * reporta a OP
     * corrente ({@code numeroOP}). Cobre tanto "a peça já está lá" quanto "acabou
     * de passar".
     */
    private boolean pecaGuardada(int posicao, int op) {
        int[] magazine = expedicaoCLP.getOrderExpedicao();
        boolean noMagazine = magazine != null
                && posicao >= 1 && posicao <= magazine.length
                && magazine[posicao - 1] == op;

        log.debug("Posição sendo guardada: {}", posicao);
        log.debug("Ordem de Produção Atual: {}", op);
        log.debug("Posições atuais do Magazine de Expedição: {}", magazine);
        log.debug("Algo foi guardado no Magazine? => [{}]", noMagazine ? "SIM" : "NÃO");

    
        
        return noMagazine || op == expedicaoCLP.getNumeroOP();
    }

    /**
     * Recompõe a fila em memória a partir do banco no boot: apenas um eventual
     * pedido em PRODUCAO
     * (órfão de reset), para a reconciliação concluí-lo. PENDENTEs NÃO voltam à
     * fila — produzir de
     * novo exige um novo POST do operador.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recomporFila() {
        pedidoRepository.findFirstByStatus(StatusPedido.PRODUCAO)
                .ifPresent(p -> pedidos.add(p.getId()));
        log.info("Fila recomposta do banco: {}", pedidos);
    }

    public void addOrder(Long id) {
        pedidos.add(id);
    }

    /**
     * Snapshot ordenado dos ids na fila (head = em produção). Para exibição no
     * frontend.
     */
    public List<Long> filaAtual() {
        return new ArrayList<>(pedidos);
    }

}
