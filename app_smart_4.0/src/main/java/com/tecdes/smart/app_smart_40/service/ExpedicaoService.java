package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.ExpedicaoDTO;
import com.tecdes.smart.app_smart_40.dto.PedidoDTO;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.repository.ExpedicaoRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        // CORRIGIDO: comparação por ID em vez de entidade não gerenciada
        if (expedicaoRepository.existsByPedidoId(pedido.getId())) {
            throw new RuntimeException("Pedido " + pedido.getId() + " já possui registro na expedição!");
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
        // ADICIONADO: registrar timestamp de entrada na expedição (exigido pelas regras
        // de negócio)
  
        return ExpedicaoDTO.fromEntity(expedicaoRepository.save(expedicao));
    }

   

    public List<ExpedicaoDTO> listarTodos() {
        return expedicaoRepository.findAll()
                .stream()
                .map(ExpedicaoDTO::fromEntity)
                .collect(Collectors.toList());
    }


    public boolean existePosicaoLivre(){
        return expedicaoRepository.countByPedidoIsNull() > 0;
    }


    public ExpedicaoDTO primeiraExpedicaoLivre(){
        return ExpedicaoDTO.fromEntity(expedicaoRepository.findFirstByPedidoIsNull().get());
    }
}