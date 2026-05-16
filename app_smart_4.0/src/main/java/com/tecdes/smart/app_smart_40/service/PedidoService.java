package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.dto.BlocoDTO;
import com.tecdes.smart.app_smart_40.dto.LaminaDTO;
import com.tecdes.smart.app_smart_40.dto.PedidoDTO;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;
    // ADICIONADO: injeção do ExpedicaoService para registrar expedição ao concluir
    private final ExpedicaoService expedicaoService;

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------
    public PedidoDTO criar(PedidoDTO dto) {

        if (!validarTipoPedido(dto)) {
            throw new IllegalArgumentException(
                    "Quantidade de blocos não corresponde ao tipo de pedido.");
        }

        List<BlocoDTO> blocoDTOs = dto.blocos();

        if (!blocosSuficientesEmEstoque(blocoDTOs)) {
            throw new IllegalArgumentException(
                    "Cores requisitadas não se encontram presentes");
        }

        if (!validarLaminas(blocoDTOs)) {
            throw new IllegalArgumentException(
                    "Lâminas propostas mal formadas, em posição incorreta ou faltante");
        }

        Pedido pedido = dto.toEntity();

        // garantir relacionamento bidirecional
        pedido.getBlocos().forEach(bloco -> {
            bloco.setPedido(pedido);
            bloco.getLaminas().forEach(lamina -> {
                lamina.setBloco(bloco);
            });
        });

        return PedidoDTO.fromEntity(pedidoRepository.save(pedido));
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    // IMPLEMENTADO: valida que cada bloco tem no máximo 3 lâminas
    // e que não há posições de lâmina duplicadas dentro do mesmo bloco
    private boolean validarLaminas(List<BlocoDTO> blocoDTOs) {
        for (BlocoDTO bloco : blocoDTOs) {
            if (bloco.laminas() == null)
                continue;

            // Máximo 3 lâminas por bloco
            if (bloco.laminas().size() > 3) {
                return false;
            }

            // Posições não podem se repetir no mesmo bloco
            long posicoesDistintas = bloco.laminas().stream()
                    .map(LaminaDTO::posicaoNoBloco)
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
    private boolean blocosSuficientesEmEstoque(List<BlocoDTO> blocos) {
        // Agrupa a quantidade necessária de cada cor solicitada no pedido
        Map<CorBloco, Long> necessario = blocos.stream()
                .collect(Collectors.groupingBy(BlocoDTO::cor, Collectors.counting()));

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

    public List<PedidoDTO> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(PedidoDTO::fromEntity)
                .toList();
    }

    public PedidoDTO buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .map(PedidoDTO::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + id));
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public PedidoDTO atualizar(Long id, PedidoDTO dto) {
        if (!pedidoRepository.existsById(id)) {
            throw new EntityNotFoundException("Pedido não encontrado: " + id);
        }

        if (!validarTipoPedido(dto)) {
            throw new IllegalArgumentException(
                    "Quantidade de blocos não corresponde ao tipo de pedido.");
        }

        Pedido pedido = dto.toEntity();
        pedido.setId(id);

        return PedidoDTO.fromEntity(pedidoRepository.save(pedido));
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

    private Boolean validarTipoPedido(PedidoDTO pedido) {
        return pedido.blocos().size() == pedido.tipoPedido().getValue();
    }

    public PedidoDTO concluir(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + id));

        // ADICIONADO: impede concluir um pedido que já está concluído
        if (pedido.getStatus() == StatusPedido.CONCLUIDO) {
            throw new IllegalStateException("Pedido " + id + " já está concluído.");
        }

        pedido.setStatus(StatusPedido.CONCLUIDO);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // ADICIONADO: registrar entrada na expedição ao concluir (regra de negócio)
        expedicaoService.registrarExpedicao(pedidoSalvo);

        return PedidoDTO.fromEntity(pedidoSalvo);
    }
}