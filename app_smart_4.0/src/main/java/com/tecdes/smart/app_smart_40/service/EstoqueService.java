package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.EstoqueRequestDTO;
import com.tecdes.smart.app_smart_40.dto.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;

    public List<EstoqueResponseDTO> getDisponivel() {
        return estoqueRepository.findByVlCorBlocoNot((byte) 0)
            .stream()
            .map(EstoqueResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    public List<EstoqueResponseDTO> getTodos() {
        return estoqueRepository.findAll()
            .stream()
            .map(EstoqueResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    public EstoqueResponseDTO adicionarBloco(EstoqueRequestDTO dto) {

        if (dto.getNrPosicao() == null) {
            throw new RuntimeException("Posição é obrigatória!");
        }

        if (dto.getVlCorBloco() == null) {
            throw new RuntimeException("Cor é obrigatória!");
        }

        if (dto.getNrPosicao() < 1 || dto.getNrPosicao() > 28) {
            throw new RuntimeException("Posição inválida! Deve ser entre 1 e 28.");
        }

        if (dto.getVlCorBloco() < 0 || dto.getVlCorBloco() > 3) {
            throw new RuntimeException("Cor inválida! Use 0=vazio, 1=preto, 2=vermelho, 3=azul.");
        }

        Estoque pos = estoqueRepository.findByNrPosicao(dto.getNrPosicao())
            .orElseThrow(() -> new RuntimeException("Posição " + dto.getNrPosicao() + " não existe!"));

        if (pos.getVlCorBloco() != 0) {
            throw new RuntimeException("Posição " + dto.getNrPosicao() + " já está ocupada!");
        }

        pos.setVlCorBloco(dto.getVlCorBloco());
        return EstoqueResponseDTO.fromEntity(estoqueRepository.save(pos));
    }

    public EstoqueResponseDTO removerBloco(Byte nrPosicao) {

        if (nrPosicao == null) {
            throw new RuntimeException("Posição é obrigatória!");
        }

        if (nrPosicao < 1 || nrPosicao > 28) {
            throw new RuntimeException("Posição inválida! Deve ser entre 1 e 28.");
        }

        Estoque pos = estoqueRepository.findByNrPosicao(nrPosicao)
            .orElseThrow(() -> new RuntimeException("Posição " + nrPosicao + " não existe!"));

        if (pos.getVlCorBloco() == 0) {
            throw new RuntimeException("Posição " + nrPosicao + " já está vazia!");
        }

        pos.setVlCorBloco((byte) 0);
        return EstoqueResponseDTO.fromEntity(estoqueRepository.save(pos));
    }
}