package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.ExpedicaoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.repository.ExpedicaoRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpedicaoService {

    private final ExpedicaoRepository expedicaoRepository;
    private final PedidoRepository pedidoRepository;

    public ExpedicaoResponseDTO registrarExpedicao(ExpedicaoRequestDTO dto) {

        if (dto.getIdPedido() == null) {
            throw new RuntimeException("ID do pedido é obrigatório!");
        }

        Pedido pedido = pedidoRepository.findById(dto.getIdPedido())
            .orElseThrow(() -> new RuntimeException("Pedido " + dto.getIdPedido() + " não encontrado!"));

        if (expedicaoRepository.existsByPedidoIdPedido(dto.getIdPedido())) {
            throw new RuntimeException("Pedido " + dto.getIdPedido() + " já possui registro na expedição!");
        }

        List<Long> posicoesOcupadas = expedicaoRepository.findAll()
            .stream()
            .map(Expedicao::getNrPosicao)
            .collect(Collectors.toList());

        Long posicaoLivre = null;
        for (long i = 1; i <= 12; i++) {
            if (!posicoesOcupadas.contains(i)) {
                posicaoLivre = i;
                break;
            }
        }

        if (posicaoLivre == null) {
            throw new RuntimeException("Expedição lotada! Todas as 12 posições estão ocupadas.");
        }

        Expedicao expedicao = new Expedicao();
        expedicao.setPedido(pedido);
        expedicao.setNrPosicao(posicaoLivre);

        return ExpedicaoResponseDTO.fromEntity(expedicaoRepository.save(expedicao));
    }

    // Chamado internamente pelo PedidoService ao concluir pedido
    public ExpedicaoResponseDTO registrarExpedicao(Pedido pedido) {
        ExpedicaoRequestDTO dto = new ExpedicaoRequestDTO(pedido.getIdPedido());
        return registrarExpedicao(dto);
    }

    public List<ExpedicaoResponseDTO> listarTodos() {
        return expedicaoRepository.findAll()
            .stream()
            .map(ExpedicaoResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }
}