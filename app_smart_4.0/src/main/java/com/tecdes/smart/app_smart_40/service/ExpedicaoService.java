package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.ExpedicaoDTO;
import com.tecdes.smart.app_smart_40.dto.PedidoDTO;
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

    public ExpedicaoDTO registrarExpedicao(ExpedicaoDTO dto) {

        if (dto.pedidoDTO() == null) {
            throw new RuntimeException("Pedido é obrigatório!");
        }

        Pedido pedido = pedidoRepository.findById(dto.pedidoDTO().id())
                .orElseThrow(() -> new RuntimeException("Pedido " + dto.pedidoDTO().id() + " não encontrado!"));

        if (expedicaoRepository.existsByPedido(dto.pedidoDTO().toEntity())) {
            throw new RuntimeException("Pedido " + dto.pedidoDTO().id() + " já possui registro na expedição!");
        }

        List<Integer> posicoesOcupadas = expedicaoRepository.findAll()
                .stream()
                .map(Expedicao::getPosicao)
                .collect(Collectors.toList());

        Integer posicaoLivre = null;
        for (Integer i = 1; i <= 12; i++) {
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
        expedicao.setPosicao(posicaoLivre);

        return ExpedicaoDTO.fromEntity(expedicaoRepository.save(expedicao));
    }

    // Chamado internamente pelo PedidoService ao concluir pedido
    public ExpedicaoDTO registrarExpedicao(Pedido pedido) {
        ExpedicaoDTO dto = ExpedicaoDTO
                .builder()
                .pedidoDTO(PedidoDTO.fromEntity(pedido))
                .build();
        return registrarExpedicao(dto);
    }

    public List<ExpedicaoDTO> listarTodos() {
        return expedicaoRepository.findAll()
                .stream()
                .map(ExpedicaoDTO::fromEntity)
                .collect(Collectors.toList());
    }
}