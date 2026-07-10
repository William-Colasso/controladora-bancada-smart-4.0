package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.dto.event.EstoqueMudou;
import com.tecdes.smart.app_smart_40.dto.request.EstoqueRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.exception.EstoqueInsuficienteException;
import com.tecdes.smart.app_smart_40.exception.PosicaoEstoqueNotFoundException;
import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;
import com.tecdes.smart.app_smart_40.service.clp.EstoqueClpWriter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    
    // Mutação → marcador p/ ClpEventoCoordinator (que decide se o grid SSE muda).
    private final ApplicationEventPublisher publisher;

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
        EstoqueResponseDTO salvo = EstoqueResponseDTO.fromEntity(estoqueRepository.save(pos));
        publisher.publishEvent(new EstoqueMudou());
        return salvo;
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
        EstoqueResponseDTO salvo = EstoqueResponseDTO.fromEntity(estoqueRepository.save(pos));
        publisher.publishEvent(new EstoqueMudou());
        return salvo;
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
        publisher.publishEvent(new EstoqueMudou()); // após commit, o coordenador reavalia o grid
    }

    /**
     * Primeira posição VAZIA do magazine de estoque, ou -1 se não houver. Consultada pelo handshake
     * do CLP (EstoqueClpService) para escolher onde guardar — mantém o acesso ao banco fora do CLP.
     */
    public int primeiraPosicaoLivre() {
        return estoqueRepository.findPosicoesVazias()
                .stream()
                .map(e -> e.getPosicao())
                .findFirst()
                .orElse(-1);
    }

}