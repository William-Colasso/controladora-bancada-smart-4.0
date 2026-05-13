package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.repository.ExpedicaoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpedicaoService {

    private final ExpedicaoRepository expedicaoRepository;

    public Expedicao registrarExpedicao(Pedido pedido) {

       
        if (expedicaoRepository.existsByPedidoIdPedido(pedido.getIdPedido())) {
            throw new RuntimeException("Pedido " + pedido.getIdPedido() + " já possui registro na expedição!");
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

        return expedicaoRepository.save(expedicao);
    }

    public List<Expedicao> listarTodos() {
        return expedicaoRepository.findAll();
    }
}