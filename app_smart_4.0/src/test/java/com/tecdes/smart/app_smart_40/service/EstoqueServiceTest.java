package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.request.EstoqueRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.BlocoResponseDTO;
import com.tecdes.smart.app_smart_40.dto.response.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstoqueService")
class EstoqueServiceTest {

    @Mock
    private EstoqueRepository estoqueRepository;

    @InjectMocks
    private EstoqueService estoqueService;

    // ---------------------------------------------------------------------------
    // Fixtures compartilhadas
    // ---------------------------------------------------------------------------

    private Estoque posicaoOcupada(long id, int posicao, CorBloco cor) {
        return new Estoque(id, posicao, cor, null);
    }

    private Estoque posicaoVazia(long id, int posicao) {
        return new Estoque(id, posicao, CorBloco.VAZIO, null);
    }

    // ===========================================================================
    // getDisponivel
    // ===========================================================================

    @Nested
    @DisplayName("getDisponivel()")
    class GetDisponivel {

        @Test
        @DisplayName("retorna lista de posições cujo corBloco não é VAZIO")
        void deveRetornarPosicoesNaoVazias() {
            // Arrange
            Estoque blocoPreto  = posicaoOcupada(1L, 5,  CorBloco.PRETO);
            Estoque blocoVermelho = posicaoOcupada(2L, 10, CorBloco.VERMELHO);
            when(estoqueRepository.findByCorBlocoNot(CorBloco.VAZIO))
                    .thenReturn(List.of(blocoPreto, blocoVermelho));

            // Act
            List<EstoqueResponseDTO> resultado = estoqueService.getDisponivel();

            // Assert
            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("retorna lista vazia quando não há posições ocupadas")
        void deveRetornarListaVaziaQuandoNenhumaPosicaoOcupada() {
            // Arrange
            when(estoqueRepository.findByCorBlocoNot(CorBloco.VAZIO))
                    .thenReturn(List.of());

            // Act
            List<EstoqueResponseDTO> resultado = estoqueService.getDisponivel();

            // Assert
            assertThat(resultado).isEmpty();
        }
    }

    // ===========================================================================
    // getTodos
    // ===========================================================================

    @Nested
    @DisplayName("getTodos()")
    class GetTodos {

        @Test
        @DisplayName("retorna todas as posições, incluindo as vazias")
        void deveRetornarTodasAsPosicoes() {
            // Arrange
            Estoque vazia   = posicaoVazia(1L, 1);
            Estoque ocupada = posicaoOcupada(2L, 2, CorBloco.AZUL);
            when(estoqueRepository.findAll()).thenReturn(List.of(vazia, ocupada));

            // Act
            List<EstoqueResponseDTO> resultado = estoqueService.getTodos();

            // Assert
            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("retorna lista vazia quando o repositório não tem registros")
        void deveRetornarListaVaziaQuandoRepositorioVazio() {
            // Arrange
            when(estoqueRepository.findAll()).thenReturn(List.of());

            // Act
            List<EstoqueResponseDTO> resultado = estoqueService.getTodos();

            // Assert
            assertThat(resultado).isEmpty();
        }
    }

    // ===========================================================================
    // adicionarBloco
    // ===========================================================================

    @Nested
    @DisplayName("adicionarBloco()")
    class AdicionarBloco {

        @Test
        @DisplayName("adiciona bloco em posição vazia e salva no repositório")
        void deveAdicionarBlocoEmPosicaoVazia() {
            // Arrange
            EstoqueRequestDTO dto   = new EstoqueRequestDTO(5, CorBloco.PRETO);
            Estoque posicaoVazia    = posicaoVazia(1L, 5);
            Estoque posicaoSalva    = posicaoOcupada(1L, 5, CorBloco.PRETO);

            when(estoqueRepository.findByPosicao(5)).thenReturn(Optional.of(posicaoVazia));
            when(estoqueRepository.save(posicaoVazia)).thenReturn(posicaoSalva);

            // Act
            EstoqueResponseDTO resultado = estoqueService.adicionarBloco(dto);

            // Assert
            assertThat(resultado.corBloco()).isEqualTo(CorBloco.PRETO);
            verify(estoqueRepository).save(posicaoVazia);
        }

        @Test
        @DisplayName("lança exceção quando posição é nula")
        void deveLancarExcecaoQuandoPosicaoNula() {
            // Arrange
            EstoqueRequestDTO dto = new EstoqueRequestDTO(null, CorBloco.PRETO);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.adicionarBloco(dto));

            assertThat(ex.getMessage()).isEqualTo("Posição é obrigatória!");
        }

        @Test
        @DisplayName("lança exceção quando cor é nula")
        void deveLancarExcecaoQuandoCorNula() {
            // Arrange
            EstoqueRequestDTO dto = new EstoqueRequestDTO(5, null);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.adicionarBloco(dto));

            assertThat(ex.getMessage()).isEqualTo("Cor é obrigatória!");
        }

        @Test
        @DisplayName("lança exceção quando posição é menor que 1")
        void deveLancarExcecaoQuandoPosicaoMenorQueUm() {
            // Arrange
            EstoqueRequestDTO dto = new EstoqueRequestDTO(0, CorBloco.PRETO);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.adicionarBloco(dto));

            assertThat(ex.getMessage()).isEqualTo("Posição inválida! Deve ser entre 1 e 28.");
        }

        @Test
        @DisplayName("lança exceção quando posição é maior que 28")
        void deveLancarExcecaoQuandoPosicaoMaiorQueVintEOito() {
            // Arrange
            EstoqueRequestDTO dto = new EstoqueRequestDTO(29, CorBloco.PRETO);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.adicionarBloco(dto));

            assertThat(ex.getMessage()).isEqualTo("Posição inválida! Deve ser entre 1 e 28.");
        }

        @Test
        @DisplayName("lança exceção quando posição não existe no repositório")
        void deveLancarExcecaoQuandoPosicaoNaoExiste() {
            // Arrange
            EstoqueRequestDTO dto = new EstoqueRequestDTO(15, CorBloco.AZUL);
            when(estoqueRepository.findByPosicao(15)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.adicionarBloco(dto));

            assertThat(ex.getMessage()).isEqualTo("Posição 15 não existe!");
        }

        @Test
        @DisplayName("lança exceção quando posição já está ocupada")
        void deveLancarExcecaoQuandoPosicaoJaOcupada() {
            // Arrange
            EstoqueRequestDTO dto        = new EstoqueRequestDTO(5, CorBloco.AZUL);
            Estoque posicaoJaOcupada    = posicaoOcupada(1L, 5, CorBloco.PRETO);

            when(estoqueRepository.findByPosicao(5)).thenReturn(Optional.of(posicaoJaOcupada));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.adicionarBloco(dto));

            assertThat(ex.getMessage()).isEqualTo("Posição 5 já está ocupada!");
        }

        @Test
        @DisplayName("não chama save quando posição já está ocupada")
        void naoDeveChamarSaveQuandoPosicaoJaOcupada() {
            // Arrange
            EstoqueRequestDTO dto     = new EstoqueRequestDTO(5, CorBloco.AZUL);
            Estoque posicaoOcupada   = posicaoOcupada(1L, 5, CorBloco.VERMELHO);

            when(estoqueRepository.findByPosicao(5)).thenReturn(Optional.of(posicaoOcupada));

            // Act
            assertThrows(RuntimeException.class, () -> estoqueService.adicionarBloco(dto));

            // Assert
            verify(estoqueRepository, never()).save(any());
        }
    }

    // ===========================================================================
    // removerBloco
    // ===========================================================================

    @Nested
    @DisplayName("removerBloco()")
    class RemoverBloco {

        @Test
        @DisplayName("remove bloco de posição ocupada, definindo cor como VAZIO")
        void deveRemoverBlocoEDefinirCorComoVazio() {
            // Arrange
            Estoque posicaoOcupada = posicaoOcupada(1L, 10, CorBloco.PRETO);
            Estoque posicaoEsvaziada = posicaoVazia(1L, 10);

            when(estoqueRepository.findByPosicao(10)).thenReturn(Optional.of(posicaoOcupada));
            when(estoqueRepository.save(posicaoOcupada)).thenReturn(posicaoEsvaziada);

            // Act
            EstoqueResponseDTO resultado = estoqueService.removerBloco((byte) 10);

            // Assert
            assertThat(resultado.corBloco()).isEqualTo(CorBloco.VAZIO);
            verify(estoqueRepository).save(posicaoOcupada);
        }

        @Test
        @DisplayName("lança exceção quando número da posição é nulo")
        void deveLancarExcecaoQuandoNrPosicaoNulo() {
            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.removerBloco(null));

            assertThat(ex.getMessage()).isEqualTo("Posição é obrigatória!");
        }

        @Test
        @DisplayName("lança exceção quando posição é menor que 1")
        void deveLancarExcecaoQuandoPosicaoMenorQueUm() {
            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.removerBloco((byte) 0));

            assertThat(ex.getMessage()).isEqualTo("Posição inválida! Deve ser entre 1 e 28.");
        }

        @Test
        @DisplayName("lança exceção quando posição é maior que 28")
        void deveLancarExcecaoQuandoPosicaoMaiorQueVintEOito() {
            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.removerBloco((byte) 29));

            assertThat(ex.getMessage()).isEqualTo("Posição inválida! Deve ser entre 1 e 28.");
        }

        @Test
        @DisplayName("lança exceção quando posição não existe no repositório")
        void deveLancarExcecaoQuandoPosicaoNaoExiste() {
            // Arrange
            when(estoqueRepository.findByPosicao(7)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.removerBloco((byte) 7));

            assertThat(ex.getMessage()).isEqualTo("Posição 7 não existe!");
        }

        @Test
        @DisplayName("lança exceção quando posição já está vazia")
        void deveLancarExcecaoQuandoPosicaoJaVazia() {
            // Arrange
            Estoque posicaoVazia = posicaoVazia(1L, 3);
            when(estoqueRepository.findByPosicao(3)).thenReturn(Optional.of(posicaoVazia));

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> estoqueService.removerBloco((byte) 3));

            assertThat(ex.getMessage()).isEqualTo("Posição 3 já está vazia!");
        }

        @Test
        @DisplayName("não chama save quando posição já está vazia")
        void naoDeveChamarSaveQuandoPosicaoJaVazia() {
            // Arrange
            Estoque posicaoVazia = posicaoVazia(1L, 3);
            when(estoqueRepository.findByPosicao(3)).thenReturn(Optional.of(posicaoVazia));

            // Act
            assertThrows(RuntimeException.class, () -> estoqueService.removerBloco((byte) 3));

            // Assert
            verify(estoqueRepository, never()).save(any());
        }
    }

    // ===========================================================================
    // retirarEstoque
    // ===========================================================================

    @Nested
    @DisplayName("retirarEstoque()")
    class RetirarEstoque {

        @Test
        @DisplayName("repassa os ids corretos ao repositório e retorna a contagem de linhas afetadas")
        void deveRepassarIdsCorretosAoRepositorio() {
            // Arrange — cria DTOs com estoques cujos ids são conhecidos
            EstoqueResponseDTO estoqueDto1 = criarEstoqueResponseDTO(10L);
            EstoqueResponseDTO estoqueDto2 = criarEstoqueResponseDTO(20L);

            BlocoResponseDTO bloco1 = new BlocoResponseDTO(1L, estoqueDto1, CorBloco.PRETO, List.of());
            BlocoResponseDTO bloco2 = new BlocoResponseDTO(2L, estoqueDto2, CorBloco.AZUL,  List.of());

            List<Long> idsEsperados = List.of(10L, 20L);
            when(estoqueRepository.retirarDoEstoque(idsEsperados)).thenReturn(2);

            // Act
            int linhasAfetadas = estoqueService.retirarEstoque(List.of(bloco1, bloco2));

            // Assert
            assertThat(linhasAfetadas).isEqualTo(2);
            verify(estoqueRepository).retirarDoEstoque(idsEsperados);
        }

        @Test
        @DisplayName("retorna zero quando lista de blocos está vazia")
        void deveRetornarZeroQuandoListaVazia() {
            // Arrange
            when(estoqueRepository.retirarDoEstoque(List.of())).thenReturn(0);

            // Act
            int linhasAfetadas = estoqueService.retirarEstoque(List.of());

            // Assert
            assertThat(linhasAfetadas).isZero();
        }

        // Método auxiliar para criar um EstoqueResponseDTO com id definido
        private EstoqueResponseDTO criarEstoqueResponseDTO(Long id) {
            // Ajuste o construtor/factory conforme a assinatura real do seu DTO
            return new EstoqueResponseDTO(id, 1, CorBloco.PRETO);
        }
    }
}