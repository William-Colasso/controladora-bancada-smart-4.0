package com.tecdes.smart.app_smart_40.service;

import com.tecdes.smart.app_smart_40.dto.event.ExpedicaoMudou;
import com.tecdes.smart.app_smart_40.dto.request.ExpedicaoRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.ExpedicaoRepository;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpedicaoService {

    private final ExpedicaoRepository expedicaoRepository;
    private final PedidoRepository pedidoRepository;
    // Mutação → marcador p/ ClpEventoCoordinator (que decide se o grid SSE muda).
    private final ApplicationEventPublisher publisher;

    public ExpedicaoResponseDTO atualizarExpedicao(Expedicao expedicao) {
        ExpedicaoResponseDTO salvo = ExpedicaoResponseDTO.fromEntity(expedicaoRepository.save(expedicao));
        publisher.publishEvent(new ExpedicaoMudou());
        return salvo;
    }

    // readOnly: mantém a sessão Hibernate aberta durante o map (inicializa o proxy lazy de Pedido).
    // Sem isso, o produtor SSE @Scheduled (sem OSIV) quebra com LazyInitializationException.
    @Transactional(readOnly = true)
    public List<ExpedicaoResponseDTO> listarTodos() {
        return expedicaoRepository.findAllComPedidoAtualEBlocos()
                .stream()
                .map(ExpedicaoResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public boolean existePosicaoLivre() {
        return expedicaoRepository.countByPedidoAtualIsNull() > 0;
    }

    public ExpedicaoResponseDTO primeiraExpedicaoLivre() {
        return ExpedicaoResponseDTO.fromEntity(expedicaoRepository.findFirstByPedidoAtualIsNull().get());
    }

    /**
     * Vincula o pedido de ordem de produção {@code ordemProducao} à posição física de expedição
     * {@code posicao}, refletindo o que o CLP da expedição reportou ter guardado.
     */
    public void guardarNaPosicao(int posicao, int ordemProducao) {
        Expedicao expedicao = expedicaoRepository.findByPosicao(posicao)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Posição de expedição " + posicao + " não existe!"));

        Pedido pedido = pedidoRepository.findByOrdemProducao(ordemProducao)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pedido com ordem de produção " + ordemProducao + " não encontrado!"));

        expedicao.setPedidoAtual(pedido);
        expedicaoRepository.save(expedicao);

        // Peça guardada na expedição = produção concluída. Ponto único de sincronização CLP→pedido;
        // feito aqui (e não em PedidoService.concluir) para evitar ciclo PedidoService↔ExpedicaoService.
        // ponytail: reusa o mesmo par de campos de concluir(); extrair helper se a regra divergir.
        if (pedido.getStatus() == StatusPedido.PRODUCAO) {
            pedido.setStatus(StatusPedido.CONCLUIDO);
            pedido.setDataEntradaExpedicao(LocalDateTime.now());
            pedidoRepository.save(pedido);
        }
        publisher.publishEvent(new ExpedicaoMudou());
    }

    /** Libera a posição física de expedição {@code posicao} (remove o pedido vinculado). */
    public void removerDaPosicao(int posicao) {
        Expedicao expedicao = expedicaoRepository.findByPosicao(posicao)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Posição de expedição " + posicao + " não existe!"));

        expedicao.setPedidoAtual(null);
        expedicaoRepository.save(expedicao);
        publisher.publishEvent(new ExpedicaoMudou());
    }
}