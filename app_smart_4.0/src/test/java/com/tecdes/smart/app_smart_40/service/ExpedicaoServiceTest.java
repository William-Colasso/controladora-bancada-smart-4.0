package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.repository.ExpedicaoRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

    @InjectMocks
    private ExpedicaoService expedicaoService;

    // ---------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------

    private Expedicao expedicaoSemPedido(Long id, int posicao) {
        return Expedicao.builder().id(id).posicao(posicao).pedido(null).build();
    }

    private Expedicao expedicaoComPedido(Long id, int posicao) {
        Pedido pedido = new Pedido();
        return Expedicao.builder().id(id).posicao(posicao).pedido(pedido).build();
    }

    // ===========================================================================
    // atualizarExpedicao
    // ===========================================================================

    @Nested
    @DisplayName("atualizarExpedicao()")
    class AtualizarExpedicao {

        @Test
        @DisplayName("salva a expedição e retorna o DTO com os dados persistidos")
        void deveSalvarExpedicaoERetornarDTO() {
            // Arrange
            Expedicao expedicao = expedicaoSemPedido(1L, 3);
            when(expedicaoRepository.save(expedicao)).thenReturn(expedicao);

            // Act
            ExpedicaoResponseDTO resultado = expedicaoService.atualizarExpedicao(expedicao);

            // Assert
            assertThat(resultado.id()).isEqualTo(1L);
            assertThat(resultado.posicao()).isEqualTo(3);
            verify(expedicaoRepository).save(expedicao);
        }

        @Test
        @DisplayName("retorna DTO com pedido nulo quando expedição não tem pedido associado")
        void deveRetornarDTOComPedidoNuloQuandoSemPedido() {
            // Arrange
            Expedicao expedicao = expedicaoSemPedido(2L, 5);
            when(expedicaoRepository.save(expedicao)).thenReturn(expedicao);

            // Act
            ExpedicaoResponseDTO resultado = expedicaoService.atualizarExpedicao(expedicao);

            // Assert
            assertThat(resultado.pedidoResponseDTO()).isNull();
        }

        @Test
        @DisplayName("retorna DTO com pedido preenchido quando expedição tem pedido associado")
        void deveRetornarDTOComPedidoQuandoExpedicaoTemPedido() {
            // Arrange
            Expedicao expedicao = expedicaoComPedido(3L, 7);
            when(expedicaoRepository.save(expedicao)).thenReturn(expedicao);

            // Act
            ExpedicaoResponseDTO resultado = expedicaoService.atualizarExpedicao(expedicao);

            // Assert
            assertThat(resultado.pedidoResponseDTO()).isNotNull();
        }
    }

    // ===========================================================================
    // listarTodos
    // ===========================================================================

    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("retorna todos os registros de expedição como DTOs")
        void deveRetornarTodasAsExpedicoes() {
            // Arrange
            Expedicao exp1 = expedicaoSemPedido(1L, 1);
            Expedicao exp2 = expedicaoComPedido(2L, 2);
            when(expedicaoRepository.findAll()).thenReturn(List.of(exp1, exp2));

            // Act
            List<ExpedicaoResponseDTO> resultado = expedicaoService.listarTodos();

            // Assert
            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("retorna lista vazia quando não há expedições cadastradas")
        void deveRetornarListaVaziaQuandoNenhumaExpedicao() {
            // Arrange
            when(expedicaoRepository.findAll()).thenReturn(List.of());

            // Act
            List<ExpedicaoResponseDTO> resultado = expedicaoService.listarTodos();

            // Assert
            assertThat(resultado).isEmpty();
        }
    }

    // ===========================================================================
    // existePosicaoLivre
    // ===========================================================================

    @Nested
    @DisplayName("existePosicaoLivre()")
    class ExistePosicaoLivre {

        @Test
        @DisplayName("retorna true quando há pelo menos uma posição sem pedido")
        void deveRetornarTrueQuandoExistePosicaoLivre() {
            // Arrange
            when(expedicaoRepository.countByPedidoIsNull()).thenReturn(3L);

            // Act
            boolean resultado = expedicaoService.existePosicaoLivre();

            // Assert
            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("retorna false quando todas as posições estão ocupadas")
        void deveRetornarFalseQuandoNenhumaPosicaoLivre() {
            // Arrange
            when(expedicaoRepository.countByPedidoIsNull()).thenReturn(0L);

            // Act
            boolean resultado = expedicaoService.existePosicaoLivre();

            // Assert
            assertThat(resultado).isFalse();
        }
    }

    // ===========================================================================
    // primeiraExpedicaoLivre
    // ===========================================================================

    @Nested
    @DisplayName("primeiraExpedicaoLivre()")
    class PrimeiraExpedicaoLivre {

        @Test
        @DisplayName("retorna DTO da primeira posição sem pedido encontrada")
        void deveRetornarDTODaPrimeiraPosicaoLivre() {
            // Arrange
            Expedicao expedicaoLivre = expedicaoSemPedido(4L, 6);
            when(expedicaoRepository.findFirstByPedidoIsNull()).thenReturn(Optional.of(expedicaoLivre));

            // Act
            ExpedicaoResponseDTO resultado = expedicaoService.primeiraExpedicaoLivre();

            // Assert
            assertThat(resultado.id()).isEqualTo(4L);
            assertThat(resultado.posicao()).isEqualTo(6);
            assertThat(resultado.pedidoResponseDTO()).isNull();
        }

        @Test
        @DisplayName("lança NoSuchElementException quando não há posição livre")
        void deveLancarExcecaoQuandoNaoHaPosicaoLivre() {
            // Arrange
            when(expedicaoRepository.findFirstByPedidoIsNull()).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(NoSuchElementException.class,
                    () -> expedicaoService.primeiraExpedicaoLivre());
        }
    }
}