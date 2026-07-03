package com.tecdes.smart.app_smart_40.service.clp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.clp.ExpedicaoCLP;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import com.tecdes.smart.app_smart_40.service.PedidoService;
import com.tecdes.smart.app_smart_40.service.SmartService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoConsumerList")
class PedidoConsumerListTest {

    @Mock
    private SmartService smartService;
    @Mock
    private ExpedicaoClpWriter expedicaoClpWriter;
    @Mock
    private PedidoService pedidoService;
    @Mock
    private PedidoRepository pedidoRepository;

    private final ExpedicaoCLP expedicaoCLP = new ExpedicaoCLP();
    private PedidoConsumerList consumer;

    @BeforeEach
    void setUp() {
        consumer.filaAtual().clear(); // fila é static — isola os testes
        consumer = new PedidoConsumerList(
                smartService, expedicaoCLP, expedicaoClpWriter, pedidoService, pedidoRepository);
    }

    private Pedido emProducao(long id, int op, int posicao) {
        return Pedido.builder()
                .id(id).ordemProducao(op).status(StatusPedido.PRODUCAO)
                .expedicao(Expedicao.builder().posicao(posicao).build())
                .blocos(List.of()).build();
    }

    private int[] magazineCom(int posicao, int op) {
        int[] mag = new int[12];
        mag[posicao - 1] = op;
        return mag;
    }

    @Test
    @DisplayName("processOrder - magazine na posição reservada mostra a OP → conclui (sobrevive a restart)")
    void processOrder_magazineComOp_conclui() {
        Pedido pedido = emProducao(5L, 42, 3);
        expedicaoCLP.setOrderExpedicao(magazineCom(3, 42));
        expedicaoCLP.setNumeroOP(0); // numeroOP já não reporta a OP — só o magazine
        when(pedidoRepository.findFirstByStatus(StatusPedido.PRODUCAO)).thenReturn(Optional.of(pedido));

        consumer.processOrder();

        verify(expedicaoClpWriter).escreverPosicao(3, 42);
        verify(pedidoService).concluir(5L);
    }

    @Test
    @DisplayName("processOrder - OP ausente do magazine e numeroOP diferente → NÃO conclui")
    void processOrder_pecaNaoGuardada_naoConclui() {
        Pedido pedido = emProducao(5L, 42, 3);
        expedicaoCLP.setOrderExpedicao(new int[12]); // vazio
        expedicaoCLP.setNumeroOP(0);
        when(pedidoRepository.findFirstByStatus(StatusPedido.PRODUCAO)).thenReturn(Optional.of(pedido));

        consumer.processOrder();

        verify(expedicaoClpWriter, never()).escreverPosicao(anyInt(), anyInt());
        verify(pedidoService, never()).concluir(any());
    }

    @Test
    @DisplayName("processOrder - numeroOP corrente casa com a OP → conclui (sem regressão do caminho antigo)")
    void processOrder_numeroOpCasa_conclui() {
        Pedido pedido = emProducao(5L, 42, 3);
        expedicaoCLP.setOrderExpedicao(new int[12]);
        expedicaoCLP.setNumeroOP(42);
        when(pedidoRepository.findFirstByStatus(StatusPedido.PRODUCAO)).thenReturn(Optional.of(pedido));

        consumer.processOrder();

        verify(expedicaoClpWriter).escreverPosicao(3, 42);
        verify(pedidoService).concluir(5L);
    }

    @Test
    @DisplayName("recomporFila - órfão em PRODUCAO na cabeça, depois PENDENTE por ordem de criação")
    void recomporFila_reconstroiDoBanco() {
        when(pedidoRepository.findFirstByStatus(StatusPedido.PRODUCAO))
                .thenReturn(Optional.of(Pedido.builder().id(10L).build()));
        when(pedidoRepository.findByStatusOrderByDataCriacaoAsc(StatusPedido.PENDENTE))
                .thenReturn(List.of(Pedido.builder().id(20L).build(), Pedido.builder().id(30L).build()));

        consumer.recomporFila();

        assertThat(consumer.filaAtual()).containsExactly(10L, 20L, 30L);
    }
}
