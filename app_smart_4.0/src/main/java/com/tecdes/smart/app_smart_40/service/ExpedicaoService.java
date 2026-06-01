package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.request.ExpedicaoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
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

    public ExpedicaoResponseDTO atualizarExpedicao(Expedicao expedicao) {
        return ExpedicaoResponseDTO.fromEntity(expedicaoRepository.save(expedicao));
    }

    public List<ExpedicaoResponseDTO> listarTodos() {
        return expedicaoRepository.findAll()
                .stream()
                .map(ExpedicaoResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public boolean existePosicaoLivre() {
        return expedicaoRepository.countByPedidoIsNull() > 0;
    }

    public ExpedicaoResponseDTO primeiraExpedicaoLivre() {
        return ExpedicaoResponseDTO.fromEntity(expedicaoRepository.findFirstByPedidoIsNull().get());
    }
}