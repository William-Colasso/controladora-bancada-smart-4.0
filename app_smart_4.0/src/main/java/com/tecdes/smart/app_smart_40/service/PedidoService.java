package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.dto.response.PedidoResponseDTO;
import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.dto.request.PedidoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.request.BlocoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.request.LaminaRequestDTO;
import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class PedidoService {

    private final EstoqueService estoqueService;
    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;
    // ADICIONADO: injeção do ExpedicaoService para registrar expedição ao concluir
    private final ExpedicaoService expedicaoService;
    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public PedidoResponseDTO criar(PedidoRequestDTO dto) {

        if (!validarTipoPedidoRequest(dto)) {
            throw new IllegalArgumentException(
                    "Quantidade de blocos não corresponde ao tipo de pedido.");
        }

        List<BlocoRequestDTO> blocoDTOs = dto.blocos();

        if (!blocosSuficientesEmEstoque(blocoDTOs)) {
            throw new IllegalArgumentException(
                    "Cores requisitadas não se encontram presentes");
        }

        if (!validarLaminas(blocoDTOs)) {
            throw new IllegalArgumentException(
                    "Lâminas propostas mal formadas, em posição incorreta ou faltante");
        }

        if (!expedicaoService.existePosicaoLivre()) {
            throw new IllegalArgumentException("Não existe posição de expedição disponível");
        }

        Pedido pedido = dto.toEntity();
        // garantir relacionamento bidirecional
        pedido.getBlocos().forEach(bloco -> {
            bloco.setPedido(pedido);
            bloco.getLaminas().forEach(lamina -> {
                lamina.setBloco(bloco);
            });
            Estoque estoque = estoqueRepository.findFirstByCorBloco(bloco.getCor());
            bloco.setEstoque(estoque);
        });

        pedido.setOrdemProducao(pedidoRepository.proximaOrdemProducao());

        ExpedicaoResponseDTO expedicaoResponseDTO = expedicaoService.primeiraExpedicaoLivre();
        Expedicao expedicao = expedicaoResponseDTO.toEntity();
        pedido.setExpedicao(expedicao);
        expedicao.setPedido(pedido);
        pedido.setStatus(StatusPedido.PENDENTE);
        System.out.println("Data de entrada: " + pedido.getDataCriacao() + "OP: " + pedido.getOrdemProducao());

        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        PedidoResponseDTO pedidoDTO = PedidoResponseDTO.fromEntity(pedidoSalvo);
        estoqueService.retirarEstoque(pedidoDTO.blocos());

        return pedidoDTO;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    // IMPLEMENTADO: valida que cada bloco tem no máximo 3 lâminas
    // e que não há posições de lâmina duplicadas dentro do mesmo bloco
    private boolean validarLaminas(List<BlocoRequestDTO> blocoDTOs) {
        for (BlocoRequestDTO bloco : blocoDTOs) {
            if (bloco.laminas() == null)
                continue;

            // Máximo 3 lâminas por bloco
            if (bloco.laminas().size() > 3) {
                return false;
            }

            // Posições não podem se repetir no mesmo bloco
            long posicoesDistintas = bloco.laminas().stream()
                    .map(LaminaRequestDTO::posicaoNoBloco)
                    .distinct()
                    .count();

            if (posicoesDistintas != bloco.laminas().size()) {
                return false;
            }
        }
        return true;
    }

    // IMPLEMENTADO: verifica se há blocos disponíveis no estoque para cada cor
    // solicitada
    private boolean blocosSuficientesEmEstoque(List<BlocoRequestDTO> blocos) {
        // Agrupa a quantidade necessária de cada cor solicitada no pedido
        Map<CorBloco, Long> necessario = blocos.stream()
                .collect(Collectors.groupingBy(BlocoRequestDTO::cor, Collectors.counting()));

        for (Map.Entry<CorBloco, Long> entry : necessario.entrySet()) {
            CorBloco cor = entry.getKey();
            long quantidadeNecessaria = entry.getValue();

            // Conta quantas posições no estoque têm essa cor disponível
            long disponivelNoEstoque = estoqueRepository.contarDisponibilidadeCor(cor);

            if (disponivelNoEstoque < quantidadeNecessaria) {
                return false;
            }
        }
        return true;
    }

    public List<PedidoResponseDTO> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(PedidoResponseDTO::fromEntity)
                .toList();
    }

    public PedidoResponseDTO buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .map(PedidoResponseDTO::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + id));
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public PedidoResponseDTO atualizar(Long id, PedidoRequestDTO dto) {
        if (!pedidoRepository.existsById(id)) {
            throw new EntityNotFoundException("Pedido não encontrado: " + id);
        }

        if (!validarTipoPedidoRequest(dto)) {
            throw new IllegalArgumentException(
                    "Quantidade de blocos não corresponde ao tipo de pedido.");
        }

        Pedido pedido = dto.toEntity();
        pedido.setId(id);

        return PedidoResponseDTO.fromEntity(pedidoRepository.save(pedido));
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    public void deletar(Long id) {
        if (!pedidoRepository.existsById(id)) {
            throw new EntityNotFoundException("Pedido não encontrado: " + id);
        }

        pedidoRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Boolean validarTipoPedidoRequest(PedidoRequestDTO pedido) {
        return pedido.blocos().size() == pedido.tipoPedido().getValue();
    }

    public PedidoResponseDTO concluir(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + id));

        // ADICIONADO: impede concluir um pedido que já está concluído
        if (pedido.getStatus() == StatusPedido.CONCLUIDO) {
            throw new IllegalStateException("Pedido " + id + " já está concluído.");
        }

        pedido.setStatus(StatusPedido.CONCLUIDO);
        pedido.setDataEntradaExpedicao(LocalDateTime.now());
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        return PedidoResponseDTO.fromEntity(pedidoSalvo);
    }
}