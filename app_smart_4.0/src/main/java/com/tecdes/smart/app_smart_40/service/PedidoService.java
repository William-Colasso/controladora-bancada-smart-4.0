package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.tecdes.smart.app_smart_40.dto.event.ExpedicaoMudou;
import com.tecdes.smart.app_smart_40.dto.response.PedidoResponseDTO;
import com.tecdes.smart.app_smart_40.exception.PedidoNotFoundException;
import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.dto.request.PedidoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.request.BlocoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.request.LaminaRequestDTO;
import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import com.tecdes.smart.app_smart_40.exception.EstoqueInsuficienteException;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;
    // Usado apenas para a checagem otimista de disponibilidade em criar();
    // a reserva/baixa de expedição em si acontece em SmartService.enviarParaProducao().
    private final ExpedicaoService expedicaoService;
    private final ApplicationEventPublisher publisher;
    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public PedidoResponseDTO criar(PedidoRequestDTO dto) {

        if (!validarTipoPedidoRequest(dto)) {
            throw new IllegalArgumentException(
                    "Quantidade de blocos não corresponde ao tipo de pedido.");
        }

        List<BlocoRequestDTO> blocoDTOs = dto.blocos();

        // CHECAGEM OTIMISTA: aqui só validamos disponibilidade — a reserva real de
        // estoque e expedição acontece em SmartService.enviarParaProducao(). Logo,
        // entre criar() e o envio à produção outro pedido pode esgotar o recurso
        // (overselling); essa falha tardia é tratada lá com EstoqueInsuficienteException.
        if (!blocosSuficientesEmEstoque(blocoDTOs)) {
            throw new EstoqueInsuficienteException(
                    "Cores requisitadas não se encontram presentes");
        }

        if (!validarLaminas(blocoDTOs)) {
            throw new IllegalArgumentException(
                    "Lâminas propostas mal formadas, em posição incorreta ou faltante");
        }

        if (!expedicaoService.existePosicaoLivre()) {
            throw new IllegalArgumentException("Não existe posição de expedição disponível");
        }

        Pedido pedido = dto.toEntity();
        // garantir relacionamento bidirecional
        pedido.getBlocos().forEach(bloco -> {
            bloco.setPedido(pedido);
            bloco.getLaminas().forEach(lamina -> {
                lamina.setBloco(bloco);
            });
           
        });

        // OP escolhida pelo usuário (valida unicidade) ou auto (MAX+1).
        pedido.setOrdemProducao(resolverOrdemProducao(dto.ordemProducao(), null));

        pedido.setStatus(StatusPedido.PENDENTE);
        System.out.println("Data de entrada: " + pedido.getDataCriacao() + "OP: " + pedido.getOrdemProducao());
        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        PedidoResponseDTO pedidoDTO = PedidoResponseDTO.fromEntity(pedidoSalvo);
        

        return pedidoDTO;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    // IMPLEMENTADO: valida que cada bloco tem no máximo 3 lâminas
    // e que não há posições de lâmina duplicadas dentro do mesmo bloco
    private boolean validarLaminas(List<BlocoRequestDTO> blocoDTOs) {
        for (BlocoRequestDTO bloco : blocoDTOs) {
            if (bloco.laminas() == null)
                continue;

            // Máximo 3 lâminas por bloco
            if (bloco.laminas().size() > 3) {
                return false;
            }

            // Posições não podem se repetir no mesmo bloco
            long posicoesDistintas = bloco.laminas().stream()
                    .map(LaminaRequestDTO::posicao)
                    .distinct()
                    .count();

            if (posicoesDistintas != bloco.laminas().size()) {
                return false;
            }
        }
        return true;
    }

    // IMPLEMENTADO: verifica se há blocos disponíveis no estoque para cada cor
    // solicitada
    private boolean blocosSuficientesEmEstoque(List<BlocoRequestDTO> blocos) {
        // Agrupa a quantidade necessária de cada cor solicitada no pedido
        Map<CorBloco, Long> necessario = blocos.stream()
                .collect(Collectors.groupingBy(BlocoRequestDTO::cor, Collectors.counting()));

        for (Map.Entry<CorBloco, Long> entry : necessario.entrySet()) {
            CorBloco cor = entry.getKey();
            long quantidadeNecessaria = entry.getValue();

            // Conta quantas posições no estoque têm essa cor disponível
            long disponivelNoEstoque = estoqueRepository.contarDisponibilidadeCor(cor);

            if (disponivelNoEstoque < quantidadeNecessaria) {
                return false;
            }
        }
        return true;
    }

    public List<PedidoResponseDTO> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(PedidoResponseDTO::fromEntity)
                .toList();
    }

    public PedidoResponseDTO buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .map(PedidoResponseDTO::fromEntity)
                .orElseThrow(() -> new PedidoNotFoundException("Pedido não encontrado: " + id));
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public PedidoResponseDTO atualizar(Long id, PedidoRequestDTO dto) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNotFoundException("Pedido não encontrado: " + id));

        // Só pedidos ainda não enviados à produção podem ser editados.
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new IllegalStateException("Só é possível editar pedidos pendentes.");
        }

        // Mesmas validações do criar() (estoque é checagem otimista — ver comentário em criar()).
        if (!validarTipoPedidoRequest(dto)) {
            throw new IllegalArgumentException(
                    "Quantidade de blocos não corresponde ao tipo de pedido.");
        }
        if (!blocosSuficientesEmEstoque(dto.blocos())) {
            throw new EstoqueInsuficienteException(
                    "Cores requisitadas não se encontram presentes");
        }
        if (!validarLaminas(dto.blocos())) {
            throw new IllegalArgumentException(
                    "Lâminas propostas mal formadas, em posição incorreta ou faltante");
        }

        // Muta in place preservando id/status/dataCriacao. A OP pode ser trocada
        // (valida unicidade, ignorando a própria OP atual); se null, preserva.
        pedido.setOrdemProducao(resolverOrdemProducao(dto.ordemProducao(), pedido.getOrdemProducao()));
        pedido.setTipoPedido(dto.tipoPedido());
        pedido.setCorTampa(dto.corTampa());

        List<Bloco> novosBlocos = dto.blocos().stream()
                .map(BlocoRequestDTO::toEntity)
                .toList();
        novosBlocos.forEach(bloco -> {
            bloco.setPedido(pedido);
            if (bloco.getLaminas() != null) {
                bloco.getLaminas().forEach(lamina -> lamina.setBloco(bloco));
            }
        });
        // clear + addAll na coleção gerenciada → orphanRemoval apaga os blocos antigos.
        pedido.getBlocos().clear();
        pedido.getBlocos().addAll(novosBlocos);

        return PedidoResponseDTO.fromEntity(pedidoRepository.save(pedido));
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    public void deletar(Long id) {
        if (!pedidoRepository.existsById(id)) {
            throw new PedidoNotFoundException("Pedido não encontrado: " + id);
        }

        pedidoRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Boolean validarTipoPedidoRequest(PedidoRequestDTO pedido) {
        return pedido.blocos().size() == pedido.tipoPedido().getValue();
    }

    // Próxima OP livre (MAX+1) — sugerida ao formulário e usada quando o usuário não escolhe.
    public Integer proximaOrdemProducao() {
        return pedidoRepository.proximaOrdemProducao();
    }

    // Resolve a OP a persistir: se o usuário escolheu uma, valida positividade e unicidade
    // (ignorando `atual`, a OP que já pertence ao próprio pedido em edição); senão, auto MAX+1.
    private Integer resolverOrdemProducao(Integer escolhida, Integer atual) {
        if (escolhida == null || escolhida.equals(atual)) {
            return atual != null ? atual : pedidoRepository.proximaOrdemProducao();
        }
        if (escolhida < 1) {
            throw new IllegalArgumentException("Ordem de produção deve ser um número positivo.");
        }
        if (pedidoRepository.existsByOrdemProducao(escolhida)) {
            throw new IllegalArgumentException("Ordem de produção " + escolhida + " já está em uso.");
        }
        return escolhida;
    }

    public PedidoResponseDTO concluir(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNotFoundException("Pedido não encontrado: " + id));

        // ADICIONADO: impede concluir um pedido que já está concluído
        if (pedido.getStatus() == StatusPedido.CONCLUIDO) {
            throw new IllegalStateException("Pedido " + id + " já está concluído.");
        }

        pedido.setStatus(StatusPedido.CONCLUIDO);
        pedido.setDataEntradaExpedicao(LocalDateTime.now());
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // O grid de expedição exibe o pedido vinculado → status mudou = grid pode mudar.
        publisher.publishEvent(new ExpedicaoMudou());

        return PedidoResponseDTO.fromEntity(pedidoSalvo);
    }
}