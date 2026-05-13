package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;

    public List<Estoque> getDisponivel() {
        return estoqueRepository.findByVlCorBlocoNot((byte) 0);
    }

    public List<Estoque> getTodos() {
        return estoqueRepository.findAll();
    }

    public Estoque adicionarBloco(Byte nrPosicao, Byte vlCorBloco) {

        // CK_ESTOQUE_POSICAO
        if (nrPosicao < 1 || nrPosicao > 28) {
            throw new RuntimeException("Posição inválida! Deve ser entre 1 e 28.");
        }

        // CK_ESTOQUE_COR_BLOCO
        if (vlCorBloco < 0 || vlCorBloco > 3) {
            throw new RuntimeException("Cor inválida! Use 0=vazio, 1=preto, 2=vermelho, 3=azul.");
        }

        Estoque pos = estoqueRepository.findByNrPosicao(nrPosicao)
            .orElseThrow(() -> new RuntimeException("Posição " + nrPosicao + " não existe!"));

        // Vínculo de Estoque
        if (pos.getVlCorBloco() != 0) {
            throw new RuntimeException("Posição " + nrPosicao + " já está ocupada!");
        }

        pos.setVlCorBloco(vlCorBloco);
        return estoqueRepository.save(pos);
    }

    public Estoque removerBloco(Byte nrPosicao) {

        // CK_ESTOQUE_POSICAO
        if (nrPosicao < 1 || nrPosicao > 28) {
            throw new RuntimeException("Posição inválida! Deve ser entre 1 e 28.");
        }

        Estoque pos = estoqueRepository.findByNrPosicao(nrPosicao)
            .orElseThrow(() -> new RuntimeException("Posição " + nrPosicao + " não existe!"));

        if (pos.getVlCorBloco() == 0) {
            throw new RuntimeException("Posição " + nrPosicao + " já está vazia!");
        }

        pos.setVlCorBloco((byte) 0);
        return estoqueRepository.save(pos);
    }
}