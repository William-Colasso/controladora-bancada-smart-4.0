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
    @DisplayName("salvarBloco: DTO válido sem lâminas → resposta deve conter id e cor corretos")
    void salvarBloco_dadoDtoValidoSemLaminas_deveRetornarIdECorNoDTO() {
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
    @DisplayName("salvarBloco: DTO com 3 lâminas válidas → repositório deve ser invocado exatamente uma vez")
    void salvarBloco_dadoDtoComTresLaminas_deveInvocarRepositorioUmaUnicaVez() {
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
    @DisplayName("salvarBloco: DTO com 3 lâminas → LaminaService deve ser chamado uma vez por lâmina")
    void salvarBloco_dadoTresLaminas_deveInvocarValidacaoTresVezes() {
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
    @DisplayName("salvarBloco: 4 lâminas → RuntimeException com mensagem de limite excedido")
    void salvarBloco_dadoQuatroLaminas_deveLancarRuntimeExceptionDeLimiteExcedido() {
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
    @DisplayName("salvarBloco: 4 lâminas → validação falha antes de chamar o repositório")
    void salvarBloco_dadoQuatroLaminas_naoDeveInvocarRepositorio() {
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


    @Test
    @DisplayName("salvarBloco: cor nula → RuntimeException com mensagem 'Cor do bloco inválida'")
    void salvarBloco_dadoCorNula_deveLancarRuntimeExceptionDeCorInvalida() {
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
    @DisplayName("salvarBloco: lâmina com cor inválida → exceção do LaminaService propagada sem invocar repositório")
    void salvarBloco_dadoLaminaComCorInvalida_devePropararExcecaoSemInvocarRepositorio() {
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