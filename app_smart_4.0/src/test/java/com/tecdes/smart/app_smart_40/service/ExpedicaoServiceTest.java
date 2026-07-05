package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.ExpedicaoRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import com.tecdes.smart.app_smart_40.model.Pedido;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpedicaoService")
public class ExpedicaoServiceTest {

    @Mock
    private ExpedicaoRepository expedicaoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher publisher;

    @InjectMocks
    private ExpedicaoService expedicaoService;

    private Expedicao expedicaoSemPedido(Long id, int posicao) {
        return Expedicao.builder().id(id).posicao(posicao).pedidoAtual(null).build();
    }

    private Expedicao expedicaoComPedido(Long id, int posicao) {
        Pedido pedido = Pedido.builder().blocos(List.of()).build();
        return Expedicao.builder().id(id).posicao(posicao).pedidoAtual(pedido).build();
    }

    // atualizarExpedicao

    @Test
    @DisplayName("atualizarExpedicao - salva e retorna DTO correto")
    void atualizarExpedicao_salvaNaRepo_retornaDTO() {
        Expedicao expedicao = expedicaoSemPedido(1L, 3);
        when(expedicaoRepository.save(expedicao)).thenReturn(expedicao);

        ExpedicaoResponseDTO resultado = expedicaoService.atualizarExpedicao(expedicao);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.posicao()).isEqualTo(3);
        verify(expedicaoRepository).save(expedicao);
    }

    @Test
    @DisplayName("atualizarExpedicao - pedido nulo quando sem pedido associado")
    void atualizarExpedicao_semPedido_retornaPedidoNulo() {
        Expedicao expedicao = expedicaoSemPedido(2L, 5);
        when(expedicaoRepository.save(expedicao)).thenReturn(expedicao);

        assertThat(expedicaoService.atualizarExpedicao(expedicao).pedidoResponseDTO()).isNull();
    }

    @Test
    @DisplayName("atualizarExpedicao - pedido preenchido quando com pedido associado")
    void atualizarExpedicao_comPedido_retornaPedidoPreenchido() {
        Expedicao expedicao = expedicaoComPedido(3L, 7);
        when(expedicaoRepository.save(expedicao)).thenReturn(expedicao);

        assertThat(expedicaoService.atualizarExpedicao(expedicao).pedidoResponseDTO()).isNotNull();
    }

    // listarTodos

    @Test
    @DisplayName("listarTodos - retorna todas as expedições")
    void listarTodos_retornaTodasAsExpedicoes() {
        when(expedicaoRepository.findAllComPedidoAtualEBlocos())
                .thenReturn(List.of(expedicaoSemPedido(1L, 1), expedicaoComPedido(2L, 2)));

        assertThat(expedicaoService.listarTodos()).hasSize(2);
    }

    @Test
    @DisplayName("listarTodos - retorna lista vazia quando não há expedições")
    void listarTodos_retornaListaVazia() {
        when(expedicaoRepository.findAllComPedidoAtualEBlocos()).thenReturn(List.of());

        assertThat(expedicaoService.listarTodos()).isEmpty();
    }

    // existePosicaoLivre

    @Test
    @DisplayName("existePosicaoLivre - true quando há posições livres")
    void existePosicaoLivre_comPosicaoLivre_retornaTrue() {
        when(expedicaoRepository.countByPedidoAtualIsNull()).thenReturn(3L);

        assertThat(expedicaoService.existePosicaoLivre()).isTrue();
    }

    @Test
    @DisplayName("existePosicaoLivre - false quando todas ocupadas")
    void existePosicaoLivre_todasOcupadas_retornaFalse() {
        when(expedicaoRepository.countByPedidoAtualIsNull()).thenReturn(0L);

        assertThat(expedicaoService.existePosicaoLivre()).isFalse();
    }

    // primeiraExpedicaoLivre

    @Test
    @DisplayName("primeiraExpedicaoLivre - retorna DTO da primeira posição livre")
    void primeiraExpedicaoLivre_encontrada_retornaDTO() {
        Expedicao livre = expedicaoSemPedido(4L, 6);
        when(expedicaoRepository.findFirstByPedidoAtualIsNull()).thenReturn(Optional.of(livre));

        ExpedicaoResponseDTO resultado = expedicaoService.primeiraExpedicaoLivre();

        assertThat(resultado.id()).isEqualTo(4L);
        assertThat(resultado.posicao()).isEqualTo(6);
        assertThat(resultado.pedidoResponseDTO()).isNull();
    }

    @Test
    @DisplayName("primeiraExpedicaoLivre - lança exceção quando não há posição livre")
    void primeiraExpedicaoLivre_semPosicaoLivre_lancaExcecao() {
        when(expedicaoRepository.findFirstByPedidoAtualIsNull()).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> expedicaoService.primeiraExpedicaoLivre());
    }

    // guardarNaPosicao — sincroniza o pedido para CONCLUIDO

    @Test
    @DisplayName("guardarNaPosicao - vincula e conclui o pedido em PRODUCAO")
    void guardarNaPosicao_pedidoEmProducao_concluiPedido() {
        Expedicao exp = expedicaoSemPedido(1L, 3);
        Pedido pedido = Pedido.builder()
                .id(5L).ordemProducao(42).status(StatusPedido.PRODUCAO).blocos(List.of()).build();
        when(expedicaoRepository.findByPosicao(3)).thenReturn(Optional.of(exp));
        when(pedidoRepository.findByOrdemProducao(42)).thenReturn(List.of(pedido));

        expedicaoService.guardarNaPosicao(3, 42);

        assertThat(exp.getPedidoAtual()).isEqualTo(pedido);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CONCLUIDO);
        assertThat(pedido.getDataEntradaExpedicao()).isNotNull();
        verify(expedicaoRepository).save(exp);
        verify(pedidoRepository).save(pedido);
    }

    @Test
    @DisplayName("guardarNaPosicao - não altera pedido que não está em PRODUCAO")
    void guardarNaPosicao_pedidoNaoEmProducao_naoConclui() {
        Expedicao exp = expedicaoSemPedido(1L, 3);
        Pedido pedido = Pedido.builder()
                .id(5L).ordemProducao(42).status(StatusPedido.CONCLUIDO).blocos(List.of()).build();
        when(expedicaoRepository.findByPosicao(3)).thenReturn(Optional.of(exp));
        when(pedidoRepository.findByOrdemProducao(42)).thenReturn(List.of(pedido));

        expedicaoService.guardarNaPosicao(3, 42);

        assertThat(exp.getPedidoAtual()).isEqualTo(pedido);
        verify(expedicaoRepository).save(exp);
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }
}