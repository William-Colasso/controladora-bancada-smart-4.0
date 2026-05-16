package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.dto.BlocoDTO;
import com.tecdes.smart.app_smart_40.dto.PedidoDTO;
import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.BlocoRepository;
import com.tecdes.smart.app_smart_40.repository.LaminaRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final BlocoRepository blocoRepository;
    private final LaminaRepository laminaRepository;

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

    private boolean validarLaminas(List<BlocoDTO> blocoDTOs) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'validarLaminas'");
    }

    private boolean blocosSuficientesEmEstoque(List<BlocoDTO> blocos) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'blocosSuficientesEmEstoque'");
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
        pedido.setId(id); // garante que vai fazer UPDATE, não INSERT

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

        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        pedido.setStatus(StatusPedido.CONCLUIDO);

        PedidoDTO pedidoDTO = PedidoDTO.fromEntity(pedido);
        return atualizar(id, pedidoDTO);

    }
}