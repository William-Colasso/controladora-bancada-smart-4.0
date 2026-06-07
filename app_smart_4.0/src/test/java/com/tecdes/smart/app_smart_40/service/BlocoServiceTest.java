package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.request.BlocoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.request.LaminaRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.BlocoResponseDTO;
import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.Lamina;
import com.tecdes.smart.app_smart_40.model.enums.AndarBloco;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.model.enums.CorLamina;
import com.tecdes.smart.app_smart_40.model.enums.PadraoLamina;
import com.tecdes.smart.app_smart_40.model.enums.PosicaoLamina;
import com.tecdes.smart.app_smart_40.repository.BlocoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlocoServiceTest {

    @Mock
    private BlocoRepository blocoRepository;

    @Mock
    private LaminaService laminaService;

    @InjectMocks
    private BlocoService blocoService;

    // bloco salvo que o repositório retorna
    private Bloco blocoSalvoBase;

    @BeforeEach
    void setUp() {
        Estoque estoque = Estoque.builder().id(1L).posicao(5).corBloco(CorBloco.PRETO).build();

        blocoSalvoBase = Bloco.builder()
                .id(1L)
                .cor(CorBloco.PRETO)
                .estoque(estoque)
                .laminas(List.of())
                .build();
    }

    // -----------------------------------------------------------------------
    // salvarBloco — caminho feliz
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("salvarBloco: dado DTO válido sem lâminas, deve retornar DTO com id e cor corretos")
    void salvarBloco_dadoDtoValidoSemLaminas_deveRetornarDtoSalvo() {
        // Arrange
        BlocoRequestDTO dto = new BlocoRequestDTO(CorBloco.PRETO, AndarBloco.PRIMEIRO, null);
        when(blocoRepository.save(any(Bloco.class))).thenReturn(blocoSalvoBase);

        // Act
        BlocoResponseDTO resultado = blocoService.salvarBloco(dto);

        // Assert
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.cor()).isEqualTo(CorBloco.PRETO);
    }

    @Test
    @DisplayName("salvarBloco: dado DTO válido com 3 lâminas, deve persistir o bloco exatamente uma vez")
    void salvarBloco_dadoDtoComTresLaminas_devePersistirUmaVez() {
        // Arrange
        List<LaminaRequestDTO> tresLaminas = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM,  PosicaoLamina.ESQUERDA),
                new LaminaRequestDTO(CorLamina.AZUL,     PadraoLamina.CASA,    PosicaoLamina.FRENTE),
                new LaminaRequestDTO(CorLamina.VERDE,    PadraoLamina.NAVIO,   PosicaoLamina.DIREITA)
        );
        BlocoRequestDTO dto = new BlocoRequestDTO(CorBloco.VERMELHO, AndarBloco.SEGUNDO, tresLaminas);

        Bloco blocoComLaminas = Bloco.builder()
                .id(2L)
                .cor(CorBloco.VERMELHO)
                .estoque(blocoSalvoBase.getEstoque())
                .laminas(List.of())
                .build();
        when(blocoRepository.save(any(Bloco.class))).thenReturn(blocoComLaminas);
        doNothing().when(laminaService).validarRegrasLamina(any());

        // Act
        blocoService.salvarBloco(dto);

        // Assert
        verify(blocoRepository, times(1)).save(any(Bloco.class));
    }

    @Test
    @DisplayName("salvarBloco: dado DTO com lâminas válidas, deve delegar validação ao LaminaService para cada lâmina")
    void salvarBloco_dadoTresLaminas_deveChamarValidacaoTresVezes() {
        // Arrange
        List<LaminaRequestDTO> tresLaminas = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA),
                new LaminaRequestDTO(CorLamina.AZUL,     PadraoLamina.CASA,   PosicaoLamina.FRENTE),
                new LaminaRequestDTO(CorLamina.VERDE,    PadraoLamina.NAVIO,  PosicaoLamina.DIREITA)
        );
        BlocoRequestDTO dto = new BlocoRequestDTO(CorBloco.AZUL, AndarBloco.TERCEIRO, tresLaminas);
        when(blocoRepository.save(any(Bloco.class))).thenReturn(blocoSalvoBase);
        doNothing().when(laminaService).validarRegrasLamina(any());

        // Act
        blocoService.salvarBloco(dto);

        // Assert
        verify(laminaService, times(3)).validarRegrasLamina(any(Lamina.class));
    }

    // -----------------------------------------------------------------------
    // salvarBloco — limite de lâminas
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("salvarBloco: dado bloco com 4 lâminas, deve lançar RuntimeException informando o limite")
    void salvarBloco_dadoQuatroLaminas_deveLancarExcecao() {
        // Arrange
        List<LaminaRequestDTO> quatroLaminas = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA),
                new LaminaRequestDTO(CorLamina.AZUL,     PadraoLamina.NENHUM, PosicaoLamina.FRENTE),
                new LaminaRequestDTO(CorLamina.VERDE,    PadraoLamina.NENHUM, PosicaoLamina.DIREITA),
                new LaminaRequestDTO(CorLamina.PRETO,    PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA)
        );
        BlocoRequestDTO dto = new BlocoRequestDTO(CorBloco.AZUL, AndarBloco.PRIMEIRO, quatroLaminas);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> blocoService.salvarBloco(dto));
        assertThat(ex.getMessage()).contains("limite permitido de 3 lâminas");
    }

    @Test
    @DisplayName("salvarBloco: dado bloco com 4 lâminas, não deve chamar o repositório")
    void salvarBloco_dadoQuatroLaminas_naoDevePersistir() {
        // Arrange
        List<LaminaRequestDTO> quatroLaminas = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA),
                new LaminaRequestDTO(CorLamina.AZUL,     PadraoLamina.NENHUM, PosicaoLamina.FRENTE),
                new LaminaRequestDTO(CorLamina.VERDE,    PadraoLamina.NENHUM, PosicaoLamina.DIREITA),
                new LaminaRequestDTO(CorLamina.PRETO,    PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA)
        );
        BlocoRequestDTO dto = new BlocoRequestDTO(CorBloco.AZUL, AndarBloco.PRIMEIRO, quatroLaminas);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> blocoService.salvarBloco(dto));
        verifyNoInteractions(blocoRepository);
    }

    // -----------------------------------------------------------------------
    // salvarBloco — cor inválida
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("salvarBloco: dado cor VAZIO (value=0), deve lançar RuntimeException de cor inválida")
    void salvarBloco_dadoCorVazio_deveLancarExcecao() {
        // Arrange — CorBloco.VAZIO tem value=0, fora do intervalo permitido (1-3)
        BlocoRequestDTO dto = new BlocoRequestDTO(CorBloco.VAZIO, AndarBloco.PRIMEIRO, null);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> blocoService.salvarBloco(dto));
        assertThat(ex.getMessage()).contains("Cor do bloco inválida");
    }

    @Test
    @DisplayName("salvarBloco: dado cor nula, deve lançar RuntimeException de cor inválida")
    void salvarBloco_dadoCorNula_deveLancarExcecao() {
        // Arrange
        BlocoRequestDTO dto = new BlocoRequestDTO(null, AndarBloco.PRIMEIRO, null);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> blocoService.salvarBloco(dto));
        assertThat(ex.getMessage()).contains("Cor do bloco inválida");
    }

    // -----------------------------------------------------------------------
    // salvarBloco — lâmina inválida propaga exceção
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("salvarBloco: dado lâmina com cor inválida, deve propagar exceção do LaminaService sem persistir")
    void salvarBloco_dadoLaminaComCorInvalida_devePropagar() {
        // Arrange
        List<LaminaRequestDTO> laminas = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.FRENTE)
        );
        BlocoRequestDTO dto = new BlocoRequestDTO(CorBloco.PRETO, AndarBloco.PRIMEIRO, laminas);

        doThrow(new RuntimeException("Erro: Cor de lâmina inválida (1-6)."))
                .when(laminaService).validarRegrasLamina(any(Lamina.class));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> blocoService.salvarBloco(dto));
        assertThat(ex.getMessage()).contains("Cor de lâmina inválida");
        verifyNoInteractions(blocoRepository);
    }
}
