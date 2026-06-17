package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.dto.request.EstoqueRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.exception.EstoqueInsuficienteException;
import com.tecdes.smart.app_smart_40.exception.PosicaoEstoqueNotFoundException;
import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;

    public List<EstoqueResponseDTO> getDisponivel() {
        return estoqueRepository.findByCorBlocoNot(CorBloco.VAZIO)
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
        if (dto.posicao() == null) {
            throw new IllegalArgumentException("Posição é obrigatória!");
        }
        if (dto.corBloco() == null) {
            throw new IllegalArgumentException("Cor é obrigatória!");
        }
        if (dto.posicao() < 1 || dto.posicao() > 28) {
            throw new IllegalArgumentException("Posição inválida! Deve ser entre 1 e 28.");
        }
        if (dto.corBloco().getValue() < 0 || dto.corBloco().getValue() > 3) {
            throw new IllegalArgumentException("Cor inválida! Use 0=vazio, 1=preto, 2=vermelho, 3=azul.");
        }

        Estoque pos = estoqueRepository.findByPosicao(dto.posicao())
                .orElseThrow(() -> new PosicaoEstoqueNotFoundException(
                        "Posição " + dto.posicao() + " não existe!"));

        pos.setCorBloco(dto.corBloco());
        return EstoqueResponseDTO.fromEntity(estoqueRepository.save(pos));
    }

    public EstoqueResponseDTO removerBloco(Byte nrPosicao) {
        if (nrPosicao == null) {
            throw new IllegalArgumentException("Posição é obrigatória!");
        }
        if (nrPosicao < 1 || nrPosicao > 28) {
            throw new IllegalArgumentException("Posição inválida! Deve ser entre 1 e 28.");
        }

        Estoque pos = estoqueRepository.findByPosicao(nrPosicao.intValue())
                .orElseThrow(() -> new PosicaoEstoqueNotFoundException(
                        "Posição " + nrPosicao + " não existe!"));

        if (pos.getCorBloco() == CorBloco.VAZIO) {
            throw new IllegalArgumentException("Posição " + nrPosicao + " já está vazia!");
        }

        pos.setCorBloco(CorBloco.VAZIO);
        return EstoqueResponseDTO.fromEntity(estoqueRepository.save(pos));
    }

    /**
     * Vincula uma posição de estoque ao bloco e dá baixa nela (marca como VAZIO).
     *
     * <p>É idempotente: se o bloco já tem estoque vinculado, não faz nada — assim
     * reenviar um pedido à produção não consome estoque duas vezes.
     *
     * <p>BLINDAGEM: a posição pode ter sido esgotada por outro pedido entre a
     * checagem otimista de {@code PedidoService.criar()} e este momento
     * (overselling); por isso o null-check lança {@link EstoqueInsuficienteException}
     * com a cor faltante (mapeada para HTTP 422 pelo GlobalExceptionHandler).
     */
    @Transactional
    public void vincularEDarBaixa(Bloco bloco) {
        if (bloco.getEstoque() != null) {
            return;
        }

        Estoque estoque = estoqueRepository.findFirstByCorBloco(bloco.getCor());
        if (estoque == null) {
            throw new EstoqueInsuficienteException(
                    "Sem estoque disponível para a cor " + bloco.getCor());
        }

        bloco.setEstoque(estoque);
        estoque.setCorBloco(CorBloco.VAZIO);
        estoqueRepository.save(estoque);
    }

}