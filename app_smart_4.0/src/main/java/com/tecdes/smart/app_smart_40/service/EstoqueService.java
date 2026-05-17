package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.dto.BlocoDTO;
import com.tecdes.smart.app_smart_40.dto.EstoqueDTO;
import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;

    public List<EstoqueDTO> getDisponivel() {
        // CORRIGIDO: retorna posições que NÃO estão vazias (cor != VAZIO), conforme
        // regra de negócio
        return estoqueRepository.findByCorBlocoNot(CorBloco.VAZIO)
                .stream()
                .map(EstoqueDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EstoqueDTO> getTodos() {
        return estoqueRepository.findAll()
                .stream()
                .map(EstoqueDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public EstoqueDTO adicionarBloco(EstoqueDTO dto) {
        if (dto.posicao() == null) {
            throw new RuntimeException("Posição é obrigatória!");
        }

        if (dto.corBloco() == null) {
            throw new RuntimeException("Cor é obrigatória!");
        }

        if (dto.posicao() < 1 || dto.posicao() > 28) {
            throw new RuntimeException("Posição inválida! Deve ser entre 1 e 28.");
        }

        if (dto.corBloco().getValue() < 0 || dto.corBloco().getValue() > 3) {
            throw new RuntimeException("Cor inválida! Use 0=vazio, 1=preto, 2=vermelho, 3=azul.");
        }

        Estoque pos = estoqueRepository.findByPosicao(dto.posicao())
                .orElseThrow(() -> new RuntimeException("Posição " + dto.posicao() + " não existe!"));

        if (pos.getCorBloco() != CorBloco.VAZIO) {
            throw new RuntimeException("Posição " + dto.posicao() + " já está ocupada!");
        }

        pos.setCorBloco(dto.corBloco());
        return EstoqueDTO.fromEntity(estoqueRepository.save(pos));
    }

    public EstoqueDTO removerBloco(Byte nrPosicao) {
        if (nrPosicao == null) {
            throw new RuntimeException("Posição é obrigatória!");
        }

        if (nrPosicao < 1 || nrPosicao > 28) {
            throw new RuntimeException("Posição inválida! Deve ser entre 1 e 28.");
        }

        Estoque pos = estoqueRepository.findByPosicao(nrPosicao.intValue())
                .orElseThrow(() -> new RuntimeException("Posição " + nrPosicao + " não existe!"));

        if (pos.getCorBloco() == CorBloco.VAZIO) {
            throw new RuntimeException("Posição " + nrPosicao + " já está vazia!");
        }

        pos.setCorBloco(CorBloco.VAZIO);
        return EstoqueDTO.fromEntity(estoqueRepository.save(pos));
    }

      public int retirarEstoque(List<BlocoDTO> blocosDTOs) {
        List<Long> blocos = blocosDTOs.stream().map(blocoDTOs -> blocoDTOs.estoque().id()).toList();
        blocos.forEach(bloco -> System.out.println(bloco));
        return estoqueRepository.retirarDoEstoque(blocos);
    }
}