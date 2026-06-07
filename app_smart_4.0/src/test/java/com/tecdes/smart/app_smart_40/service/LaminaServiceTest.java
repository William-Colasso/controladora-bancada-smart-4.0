package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.request.LaminaRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.LaminaResponseDTO;
import com.tecdes.smart.app_smart_40.model.Lamina;
import com.tecdes.smart.app_smart_40.model.enums.CorLamina;
import com.tecdes.smart.app_smart_40.model.enums.PadraoLamina;
import com.tecdes.smart.app_smart_40.model.enums.PosicaoLamina;
import com.tecdes.smart.app_smart_40.repository.LaminaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaminaServiceTest {

    @Mock
    private LaminaRepository laminaRepository;

    @InjectMocks
    private LaminaService laminaService;

    private Lamina laminaRetornadaDoRepositorio;

    @BeforeEach
    void setUp() {
        laminaRetornadaDoRepositorio = Lamina.builder()
                .id(1L)
                .cor(CorLamina.VERMELHO)
                .padrao(PadraoLamina.CASA)
                .posicao(PosicaoLamina.FRENTE)
                .build();
    }

    // -----------------------------------------------------------------------
    // salvar — caminho feliz
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("salvar: dado DTO válido, deve retornar DTO com os dados da lâmina salva")
    void salvar_dadoDtoValido_deveRetornarDtoSalvo() {
        // Arrange
        LaminaRequestDTO dto = new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.CASA, PosicaoLamina.FRENTE);
        when(laminaRepository.save(any(Lamina.class))).thenReturn(laminaRetornadaDoRepositorio);

        // Act
        LaminaResponseDTO resultado = laminaService.salvar(dto);

        // Assert
        assertThat(resultado.cor()).isEqualTo(CorLamina.VERMELHO);
        assertThat(resultado.padrao()).isEqualTo(PadraoLamina.CASA);
        assertThat(resultado.posicaoNoBloco()).isEqualTo(PosicaoLamina.FRENTE);
    }

    @Test
    @DisplayName("salvar: dado DTO válido, deve chamar o repositório exatamente uma vez")
    void salvar_dadoDtoValido_devePersistirUmaVez() {
        // Arrange
        LaminaRequestDTO dto = new LaminaRequestDTO(CorLamina.AZUL, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA);
        when(laminaRepository.save(any(Lamina.class))).thenReturn(laminaRetornadaDoRepositorio);

        // Act
        laminaService.salvar(dto);

        // Assert
        verify(laminaRepository, times(1)).save(any(Lamina.class));
    }

    // -----------------------------------------------------------------------
    // validarRegrasLamina — lâmina completamente válida
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("validarRegrasLamina: dado lâmina com todos os campos válidos, não deve lançar exceção")
    void validarRegrasLamina_dadoLaminaValida_naoDeveLancarExcecao() {
        // Arrange
        Lamina lamina = Lamina.builder()
                .cor(CorLamina.BRANCO)
                .padrao(PadraoLamina.ESTRELA)
                .posicao(PosicaoLamina.DIREITA)
                .build();

        // Act & Assert — nenhuma exceção esperada
        laminaService.validarRegrasLamina(lamina);
    }

    // -----------------------------------------------------------------------
    // validarRegrasLamina — cor inválida
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("validarRegrasLamina: dado cor nula, deve lançar RuntimeException de cor inválida")
    void validarRegrasLamina_dadoCorNula_deveLancarExcecao() {
        // Arrange
        Lamina lamina = Lamina.builder()
                .cor(null)
                .padrao(PadraoLamina.NENHUM)
                .posicao(PosicaoLamina.FRENTE)
                .build();

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> laminaService.validarRegrasLamina(lamina));
        assertThat(ex.getMessage()).contains("Cor de lâmina inválida");
    }

    // -----------------------------------------------------------------------
    // validarRegrasLamina — posição inválida
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("validarRegrasLamina: dado posição nula, deve lançar RuntimeException de posição inválida")
    void validarRegrasLamina_dadoPosicaoNula_deveLancarExcecao() {
        // Arrange
        Lamina lamina = Lamina.builder()
                .cor(CorLamina.VERDE)
                .padrao(PadraoLamina.NENHUM)
                .posicao(null)
                .build();

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> laminaService.validarRegrasLamina(lamina));
        assertThat(ex.getMessage()).contains("Posição da lâmina inválida");
    }

    // -----------------------------------------------------------------------
    // validarRegrasLamina — padrão nulo causa NPE (gap documentado)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("validarRegrasLamina: dado padrão nulo, deve lançar NullPointerException — gap: falta null-check no service")
    void validarRegrasLamina_dadoPadraoNulo_deveLancarNullPointerException() {
        // Arrange — PadraoLamina nulo dispara NPE antes da mensagem amigável
        // porque o código chama lamina.getPadrao().getValue() sem null-check
        Lamina lamina = Lamina.builder()
                .cor(CorLamina.PRETO)
                .padrao(null)
                .posicao(PosicaoLamina.DIREITA)
                .build();

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> laminaService.validarRegrasLamina(lamina));
    }

    // -----------------------------------------------------------------------
    // salvar — repositório não é chamado quando validação falha
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("salvar: dado cor nula, não deve chamar o repositório")
    void salvar_dadoCorNula_naoDevePersistir() {
        // Arrange
        LaminaRequestDTO dto = new LaminaRequestDTO(null, PadraoLamina.NENHUM, PosicaoLamina.FRENTE);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> laminaService.salvar(dto));
        verifyNoInteractions(laminaRepository);
    }

    @Test
    @DisplayName("salvar: dado posição nula, não deve chamar o repositório")
    void salvar_dadoPosicaoNula_naoDevePersistir() {
        // Arrange
        LaminaRequestDTO dto = new LaminaRequestDTO(CorLamina.AZUL, PadraoLamina.NENHUM, null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> laminaService.salvar(dto));
        verifyNoInteractions(laminaRepository);
    }
}
