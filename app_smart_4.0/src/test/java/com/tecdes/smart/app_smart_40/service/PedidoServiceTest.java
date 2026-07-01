package com.tecdes.smart.app_smart_40.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tecdes.smart.app_smart_40.dto.request.*;
import com.tecdes.smart.app_smart_40.dto.response.*;
import com.tecdes.smart.app_smart_40.model.*;
import com.tecdes.smart.app_smart_40.model.enums.*;
import com.tecdes.smart.app_smart_40.exception.PedidoNotFoundException;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private EstoqueRepository estoqueRepository;

    @Mock
    private ExpedicaoService expedicaoService;

    @InjectMocks
    private PedidoService pedidoService;

    // =========================================================================
    // TESTES: criar(PedidoRequestDTO dto) - Happy Cases
    // =========================================================================

    @Test
    @DisplayName("Deve criar pedido SIMPLES com 1 bloco com sucesso")
    void deveCriarPedidoSimples_QuandoDadosValidos() {
        // Arrange
        PedidoRequestDTO dto = buildPedidoRequestDTOSimples();
        Pedido pedidoEntidade = buildPedidoEntidade(1L, TipoPedido.SIMPLES, StatusPedido.PENDENTE);

        when(expedicaoService.existePosicaoLivre()).thenReturn(true);
        when(estoqueRepository.contarDisponibilidadeCor(CorBloco.PRETO)).thenReturn(1L);
        when(pedidoRepository.proximaOrdemProducao()).thenReturn(1);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoEntidade);

        // Act
        PedidoResponseDTO resultado = pedidoService.criar(dto);

        // Assert
        assertNotNull(resultado);
        assertEquals(1L, resultado.id());
        assertEquals(TipoPedido.SIMPLES, resultado.tipoPedido());
        assertEquals(StatusPedido.PENDENTE, resultado.status());
        // criar() apenas valida e persiste o pedido (status PENDENTE). A reserva de
        // expedição e a baixa de estoque ocorrem em SmartService.enviarParaProducao().
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Deve criar pedido DUPLO com 2 blocos com sucesso")
    void deveCriarPedidoDuplo_QuandoDadosValidos() {
        // Arrange
        PedidoRequestDTO dto = buildPedidoRequestDTODuplo();
        Pedido pedidoEntidade = buildPedidoEntidade(2L, TipoPedido.DUPLO, StatusPedido.PENDENTE);

        when(expedicaoService.existePosicaoLivre()).thenReturn(true);
        when(estoqueRepository.contarDisponibilidadeCor(any(CorBloco.class))).thenReturn(2L);
        when(pedidoRepository.proximaOrdemProducao()).thenReturn(2);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoEntidade);

        // Act
        PedidoResponseDTO resultado = pedidoService.criar(dto);

        // Assert
        assertNotNull(resultado);
        assertEquals(2L, resultado.id());
        assertEquals(TipoPedido.DUPLO, resultado.tipoPedido());
        assertEquals(2, resultado.blocos().size());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Deve criar pedido TRIPLO com 3 blocos com sucesso")
    void deveCriarPedidoTriplo_QuandoDadosValidos() {
        // Arrange
        PedidoRequestDTO dto = buildPedidoRequestDTOTriplo();
        Pedido pedidoEntidade = buildPedidoEntidade(3L, TipoPedido.TRIPLO, StatusPedido.PENDENTE);

        when(expedicaoService.existePosicaoLivre()).thenReturn(true);
        when(estoqueRepository.contarDisponibilidadeCor(any(CorBloco.class))).thenReturn(3L);
        when(pedidoRepository.proximaOrdemProducao()).thenReturn(3);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoEntidade);

        // Act
        PedidoResponseDTO resultado = pedidoService.criar(dto);

        // Assert
        assertNotNull(resultado);
        assertEquals(3L, resultado.id());
        assertEquals(TipoPedido.TRIPLO, resultado.tipoPedido());
        assertEquals(3, resultado.blocos().size());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    // =========================================================================
    // TESTES: criar(PedidoRequestDTO dto) - Bad Cases
    // =========================================================================

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando SIMPLES com 2 blocos")
    void deveRejeitar_QuandoTipoSimplesComDoisBlocos() {
        // Arrange
        PedidoRequestDTO dto = buildPedidoRequestDTOInvalido(TipoPedido.SIMPLES, 2);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criar(dto),
                "Quantidade de blocos não corresponde ao tipo de pedido.");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando DUPLO com 1 bloco")
    void deveRejeitar_QuandoTipoDuploComUmBloco() {
        // Arrange
        PedidoRequestDTO dto = buildPedidoRequestDTOInvalido(TipoPedido.DUPLO, 1);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criar(dto),
                "Quantidade de blocos não corresponde ao tipo de pedido.");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando cor do bloco indisponível no estoque")
    void deveRejeitar_QuandoCorBlocoBlocoIndisponivel() {
        // Arrange
        PedidoRequestDTO dto = buildPedidoRequestDTOSimples();

        when(estoqueRepository.contarDisponibilidadeCor(CorBloco.PRETO)).thenReturn(0L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criar(dto),
                "Cores requisitadas não se encontram presentes");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando sem posição de expedição disponível")
    void deveRejeitar_QuandoSemPosicaoExpedicao() {
        // Arrange
        PedidoRequestDTO dto = buildPedidoRequestDTOSimples();

        when(estoqueRepository.contarDisponibilidadeCor(CorBloco.PRETO)).thenReturn(1L);
        when(expedicaoService.existePosicaoLivre()).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criar(dto),
                "Não existe posição de expedição disponível");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando bloco tem lâminas em posições duplicadas")
    void deveRejeitar_QuandoBlocoComLaminasEmPosicoesOuplicadas() {
        // Arrange
        List<LaminaRequestDTO> laminasComDuplicatas = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA),
                new LaminaRequestDTO(CorLamina.AZUL, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA));
        BlocoRequestDTO blocoDuplicado = new BlocoRequestDTO(CorBloco.PRETO, AndarBloco.PRIMEIRO, laminasComDuplicatas);
        PedidoRequestDTO dto = new PedidoRequestDTO(TipoPedido.SIMPLES, CorTampa.PRETO, null,
                List.of(blocoDuplicado));

        when(estoqueRepository.contarDisponibilidadeCor(CorBloco.PRETO)).thenReturn(1L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criar(dto),
                "Lâminas propostas mal formadas, em posição incorreta ou faltante");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando bloco tem mais de 3 lâminas")
    void deveRejeitar_QuandoBlocoComMaisDeTresLaminas() {
        // Arrange
        List<LaminaRequestDTO> laminasExcesso = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA),
                new LaminaRequestDTO(CorLamina.AZUL, PadraoLamina.NENHUM, PosicaoLamina.FRENTE),
                new LaminaRequestDTO(CorLamina.AMARELO, PadraoLamina.NENHUM, PosicaoLamina.DIREITA),
                new LaminaRequestDTO(CorLamina.VERDE, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA));
        BlocoRequestDTO blocoExcesso = new BlocoRequestDTO(CorBloco.PRETO, AndarBloco.PRIMEIRO, laminasExcesso);
        PedidoRequestDTO dto = new PedidoRequestDTO(TipoPedido.SIMPLES, CorTampa.PRETO, null,
                List.of(blocoExcesso));

        when(estoqueRepository.contarDisponibilidadeCor(CorBloco.PRETO)).thenReturn(1L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criar(dto),
                "Lâminas propostas mal formadas, em posição incorreta ou faltante");
    }

    // =========================================================================
    // TESTES: listarTodos() - Happy Cases
    // =========================================================================

    @Test
    @DisplayName("Deve retornar lista com múltiplos pedidos mapeados para DTO")
    void deveRetornarListaMultiplosPedidos_QuandoExistemPedidos() {
        // Arrange
        Pedido pedido1 = buildPedidoEntidade(1L, TipoPedido.SIMPLES, StatusPedido.PENDENTE);
        Pedido pedido2 = buildPedidoEntidade(2L, TipoPedido.DUPLO, StatusPedido.PRODUCAO);
        List<Pedido> pedidosLista = List.of(pedido1, pedido2);

        when(pedidoRepository.findAll()).thenReturn(pedidosLista);

        // Act
        List<PedidoResponseDTO> resultado = pedidoService.listarTodos();

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(1L, resultado.get(0).id());
        assertEquals(2L, resultado.get(1).id());
        verify(pedidoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há pedidos")
    void deveRetornarListaVazia_QuandoNaoExistemPedidos() {
        // Arrange
        when(pedidoRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<PedidoResponseDTO> resultado = pedidoService.listarTodos();

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(pedidoRepository, times(1)).findAll();
    }

    // =========================================================================
    // TESTES: buscarPorId(Long id) - Happy Cases
    // =========================================================================

    @Test
    @DisplayName("Deve retornar o DTO correto quando o pedido existe")
    void deveRetornarPedidoDTO_QuandoPedidoExiste() {
        // Arrange
        Long pedidoId = 1L;
        Pedido pedido = buildPedidoEntidade(pedidoId, TipoPedido.SIMPLES, StatusPedido.PENDENTE);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        // Act
        PedidoResponseDTO resultado = pedidoService.buscarPorId(pedidoId);

        // Assert
        assertNotNull(resultado);
        assertEquals(pedidoId, resultado.id());
        assertEquals(TipoPedido.SIMPLES, resultado.tipoPedido());
        assertEquals(StatusPedido.PENDENTE, resultado.status());
        verify(pedidoRepository, times(1)).findById(pedidoId);
    }

    // =========================================================================
    // TESTES: buscarPorId(Long id) - Bad Cases
    // =========================================================================

    @Test
    @DisplayName("Deve lançar EntityNotFoundException quando o pedido não existe")
    void deveLancarException_QuandoPedidoNaoExiste() {
        // Arrange
        Long pedidoId = 999L;

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> pedidoService.buscarPorId(pedidoId),
                "Pedido não encontrado: " + pedidoId);
        verify(pedidoRepository, times(1)).findById(pedidoId);
    }

    // =========================================================================
    // TESTES: concluir(Long id) - Happy Cases
    // =========================================================================

    @Test
    @DisplayName("Deve mudar status para CONCLUIDO e definir dataEntradaExpedicao")
    void deveConluirPedido_QuandoPedidoExisteEstaoPendente() {
        // Arrange
        Long pedidoId = 1L;
        Pedido pedido = buildPedidoEntidade(pedidoId, TipoPedido.SIMPLES, StatusPedido.PENDENTE);
        Pedido pedidoAtualizado = buildPedidoEntidade(pedidoId, TipoPedido.SIMPLES, StatusPedido.CONCLUIDO);
        pedidoAtualizado.setDataEntradaExpedicao(LocalDateTime.now());

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoAtualizado);

        // Act
        PedidoResponseDTO resultado = pedidoService.concluir(pedidoId);

        // Assert
        assertNotNull(resultado);
        assertEquals(StatusPedido.CONCLUIDO, resultado.status());
        assertNotNull(resultado.dataEntradaExpedicao());
        verify(pedidoRepository, times(1)).findById(pedidoId);
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    // =========================================================================
    // TESTES: concluir(Long id) - Bad Cases
    // =========================================================================

    @Test
    @DisplayName("Deve lançar EntityNotFoundException quando pedido não existe")
    void deveLancarExceptionConcluir_QuandoPedidoNaoExiste() {
        // Arrange
        Long pedidoId = 999L;

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> pedidoService.concluir(pedidoId),
                "Pedido não encontrado: " + pedidoId);
        verify(pedidoRepository, times(1)).findById(pedidoId);
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando pedido já está CONCLUIDO")
    void deveLancarExceptionConcluir_QuandoPedidoJaEstaCompleto() {
        // Arrange
        Long pedidoId = 1L;
        Pedido pedido = buildPedidoEntidade(pedidoId, TipoPedido.SIMPLES, StatusPedido.CONCLUIDO);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> pedidoService.concluir(pedidoId),
                "Pedido " + pedidoId + " já está concluído.");
        verify(pedidoRepository, times(1)).findById(pedidoId);
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // =========================================================================
    // TESTES: atualizar(Long id, PedidoRequestDTO dto)
    // =========================================================================

    @Test
    @DisplayName("atualizar - edita pedido PENDENTE preservando OP/status/dataCriacao e troca os blocos")
    void deveAtualizar_QuandoPedidoPendente() {
        Long id = 1L;
        Pedido existente = buildPedidoEntidade(id, TipoPedido.SIMPLES, StatusPedido.PENDENTE);
        existente.setOrdemProducao(42);
        LocalDateTime criacao = existente.getDataCriacao();
        PedidoRequestDTO dto = buildPedidoRequestDTOSimples();

        when(pedidoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(estoqueRepository.contarDisponibilidadeCor(CorBloco.PRETO)).thenReturn(1L);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoResponseDTO resultado = pedidoService.atualizar(id, dto);

        assertEquals(42, resultado.ordemProducao());           // OP preservada
        assertEquals(StatusPedido.PENDENTE, resultado.status()); // status preservado
        assertEquals(criacao, resultado.dataCriacao());          // dataCriacao preservada
        assertEquals(1, resultado.blocos().size());              // blocos substituídos
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    @DisplayName("atualizar - rejeita com IllegalStateException quando pedido não está PENDENTE")
    void deveRejeitarAtualizar_QuandoNaoPendente() {
        Long id = 1L;
        Pedido existente = buildPedidoEntidade(id, TipoPedido.SIMPLES, StatusPedido.PRODUCAO);

        when(pedidoRepository.findById(id)).thenReturn(Optional.of(existente));

        assertThrows(IllegalStateException.class,
                () -> pedidoService.atualizar(id, buildPedidoRequestDTOSimples()),
                "Só é possível editar pedidos pendentes.");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("atualizar - lança PedidoNotFoundException quando pedido não existe")
    void deveRejeitarAtualizar_QuandoPedidoNaoExiste() {
        Long id = 999L;

        when(pedidoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(PedidoNotFoundException.class,
                () -> pedidoService.atualizar(id, buildPedidoRequestDTOSimples()));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // =========================================================================
    // TESTES: OP escolhida pelo usuário (criar)
    // =========================================================================

    @Test
    @DisplayName("Deve usar a OP escolhida quando informada, positiva e única")
    void deveUsarOpEscolhida_QuandoUnicaEValida() {
        PedidoRequestDTO dto = buildPedidoRequestDTOComOp(42);
        when(expedicaoService.existePosicaoLivre()).thenReturn(true);
        when(estoqueRepository.contarDisponibilidadeCor(CorBloco.PRETO)).thenReturn(1L);
        when(pedidoRepository.existsByOrdemProducao(42)).thenReturn(false);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.criar(dto);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertEquals(42, captor.getValue().getOrdemProducao());
        verify(pedidoRepository, never()).proximaOrdemProducao();
    }

    @Test
    @DisplayName("Deve rejeitar quando a OP escolhida já está em uso")
    void deveRejeitar_QuandoOpEscolhidaDuplicada() {
        PedidoRequestDTO dto = buildPedidoRequestDTOComOp(42);
        when(expedicaoService.existePosicaoLivre()).thenReturn(true);
        when(estoqueRepository.contarDisponibilidadeCor(CorBloco.PRETO)).thenReturn(1L);
        when(pedidoRepository.existsByOrdemProducao(42)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> pedidoService.criar(dto));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Deve usar a próxima OP automática quando o usuário não escolhe (null)")
    void deveUsarProximaOp_QuandoOpNula() {
        PedidoRequestDTO dto = buildPedidoRequestDTOComOp(null);
        when(expedicaoService.existePosicaoLivre()).thenReturn(true);
        when(estoqueRepository.contarDisponibilidadeCor(CorBloco.PRETO)).thenReturn(1L);
        when(pedidoRepository.proximaOrdemProducao()).thenReturn(7);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.criar(dto);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getOrdemProducao());
        verify(pedidoRepository, never()).existsByOrdemProducao(any());
    }

    // =========================================================================
    // MÉTODOS AUXILIARES (Factories)
    // =========================================================================

    private static PedidoRequestDTO buildPedidoRequestDTOComOp(Integer op) {
        List<LaminaRequestDTO> laminas = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA));
        BlocoRequestDTO bloco = new BlocoRequestDTO(CorBloco.PRETO, AndarBloco.PRIMEIRO, laminas);
        return new PedidoRequestDTO(TipoPedido.SIMPLES, CorTampa.PRETO, op, List.of(bloco));
    }

    private static PedidoRequestDTO buildPedidoRequestDTOSimples() {
        List<LaminaRequestDTO> laminas = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA));
        BlocoRequestDTO bloco = new BlocoRequestDTO(CorBloco.PRETO, AndarBloco.PRIMEIRO, laminas);
        return new PedidoRequestDTO(TipoPedido.SIMPLES, CorTampa.PRETO, null, List.of(bloco));
    }

    private static PedidoRequestDTO buildPedidoRequestDTODuplo() {
        List<LaminaRequestDTO> laminas1 = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA));
        List<LaminaRequestDTO> laminas2 = List.of(
                new LaminaRequestDTO(CorLamina.AZUL, PadraoLamina.NENHUM, PosicaoLamina.FRENTE));
        BlocoRequestDTO bloco1 = new BlocoRequestDTO(CorBloco.PRETO, AndarBloco.PRIMEIRO, laminas1);
        BlocoRequestDTO bloco2 = new BlocoRequestDTO(CorBloco.VERMELHO, AndarBloco.SEGUNDO, laminas2);
        return new PedidoRequestDTO(TipoPedido.DUPLO, CorTampa.PRETO, null, List.of(bloco1, bloco2));
    }

    private static PedidoRequestDTO buildPedidoRequestDTOTriplo() {
        List<LaminaRequestDTO> laminas1 = List.of(
                new LaminaRequestDTO(CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA));
        List<LaminaRequestDTO> laminas2 = List.of(
                new LaminaRequestDTO(CorLamina.AZUL, PadraoLamina.NENHUM, PosicaoLamina.FRENTE));
        List<LaminaRequestDTO> laminas3 = List.of(
                new LaminaRequestDTO(CorLamina.AMARELO, PadraoLamina.NENHUM, PosicaoLamina.DIREITA));
        BlocoRequestDTO bloco1 = new BlocoRequestDTO(CorBloco.PRETO, AndarBloco.PRIMEIRO, laminas1);
        BlocoRequestDTO bloco2 = new BlocoRequestDTO(CorBloco.VERMELHO, AndarBloco.SEGUNDO, laminas2);
        BlocoRequestDTO bloco3 = new BlocoRequestDTO(CorBloco.AZUL, AndarBloco.TERCEIRO, laminas3);
        return new PedidoRequestDTO(TipoPedido.TRIPLO, CorTampa.PRETO, null,
                List.of(bloco1, bloco2, bloco3));
    }

    private static PedidoRequestDTO buildPedidoRequestDTOInvalido(TipoPedido tipo, int quantidadeBlocos) {
        List<BlocoRequestDTO> blocos = new java.util.ArrayList<>();
        for (int i = 0; i < quantidadeBlocos; i++) {
            List<LaminaRequestDTO> laminas = List.of(new LaminaRequestDTO(
                    CorLamina.VERMELHO, PadraoLamina.NENHUM, PosicaoLamina.ESQUERDA));
            blocos.add(new BlocoRequestDTO(CorBloco.PRETO, AndarBloco.PRIMEIRO, laminas));
        }
        return new PedidoRequestDTO(tipo, CorTampa.PRETO, null, blocos);
    }

    private static Pedido buildPedidoEntidade(Long id, TipoPedido tipo, StatusPedido status) {
        Estoque estoque = Estoque.builder()
                .id(1L)
                .posicao(1)
                .corBloco(CorBloco.PRETO)
                .blocos(Collections.emptyList())
                .build();

        List<Bloco> blocos = new java.util.ArrayList<>();
        int quantidadeBlocos = tipo.getValue();

        for (int i = 0; i < quantidadeBlocos; i++) {
            Bloco bloco = Bloco.builder()
                    .id((long) (i + 1))
                    .cor(CorBloco.PRETO)
                    .andar(AndarBloco.PRIMEIRO)
                    .laminas(Collections.emptyList())
                    .estoque(estoque)
                    .build();
            blocos.add(bloco);
        }

        return Pedido.builder()
                .id(id)
                .ordemProducao(id.intValue())
                .status(status)
                .tipoPedido(tipo)
                .corTampa(CorTampa.PRETO)
                .dataCriacao(LocalDateTime.now())
                .blocos(blocos)
                .build();
    }

    private static ExpedicaoResponseDTO buildExpedicaoResponseDTO() {
        return new ExpedicaoResponseDTO(1L, 1, null);
    }

    private static Expedicao buildExpedicao() {
        return Expedicao.builder()
                .id(1L)
                .posicao(1)
                .build();
    }
}
