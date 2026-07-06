package com.tecdes.smart.app_smart_40.service.sse;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoAllData;
import com.tecdes.smart.app_smart_40.dto.event.EstacaoHeartbeat;
import com.tecdes.smart.app_smart_40.dto.event.EstacaoStatusEvent;
import com.tecdes.smart.app_smart_40.dto.event.EstoqueGridEvent;
import com.tecdes.smart.app_smart_40.dto.event.EstoqueMudou;
import com.tecdes.smart.app_smart_40.dto.event.ExpedicaoGridEvent;
import com.tecdes.smart.app_smart_40.dto.event.ExpedicaoMudou;
import com.tecdes.smart.app_smart_40.dto.response.EstoqueResponseDTO;
import com.tecdes.smart.app_smart_40.dto.response.ExpedicaoResponseDTO;
import com.tecdes.smart.app_smart_40.model.clp.EstacaoCLP;
import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.service.EstoqueService;
import com.tecdes.smart.app_smart_40.service.ExpedicaoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import tools.jackson.databind.ObjectMapper;

/**
 * Coordenador central de eventos do loop CLP↔Backend↔Frontend.
 *
 * <p><b>Regra única:</b> um evento SSE só é publicado quando as variáveis das quais ele depende
 * mudaram. Esta classe é o único ponto que decide "mudou → publica" (substitui os antigos
 * producers agendados, que cada um pollava e cacheava por conta própria):
 *
 * <ul>
 *   <li><b>{@code estacao-all} / {@code estacao-status}:</b> o write path chama
 *       {@link #aoPassadaLida} após cada passada lida com sucesso. O coordenador compara a
 *       assinatura JSON do bean {@code *CLP} (singleton mutável — não há equals por valor) e o
 *       status derivado com os últimos publicados; só o que mudou vira evento. O heartbeat
 *       (liveness) pulsa em toda passada — é efêmero por definição.</li>
 *   <li><b>Grids (banco):</b> os services de mutação publicam os marcadores {@link EstoqueMudou}/
 *       {@link ExpedicaoMudou} (após commit); o coordenador re-consulta o grid e publica só se o
 *       conteúdo diferiu do último emitido. Sem polling de banco.</li>
 *   <li><b>Watchdog offline:</b> sem leitura há mais de {@link #FRESCOR_MS} ms, publica
 *       {@code estado="off"} para as 4 estações (on-change) — preserva a UX de "sem comunicação"
 *       que antes dependia do gate de frescor dos producers.</li>
 * </ul>
 *
 * <p>Nunca toca o socket S7 nem muta os beans {@code *CLP} — só lê o que o write path preencheu.
 * Métodos {@code synchronized}: passadas (thread do scheduler), watchdog e listeners de mutação
 * compartilham os caches de última emissão.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClpEventoCoordinator {

    /** Janela de frescor: sem leitura há mais que isso, as estações são consideradas offline. */
    private static final long FRESCOR_MS = 1500;

    private final ApplicationEventPublisher publisher;
    private final SseEmitterRegistry sseRegistry;
    private final EstadoProducaoService estado;
    private final EstoqueService estoqueService;
    private final ExpedicaoService expedicaoService;
    private final ObjectMapper mapper;

    // ── Última emissão por dependência (a "memória" que define se algo mudou) ──
    private final Map<EstacoesCLP, String> ultimoJsonPorEstacao = new EnumMap<>(EstacoesCLP.class);
    private final Map<EstacoesCLP, EstacaoStatusEvent> ultimoStatusPorEstacao = new EnumMap<>(EstacoesCLP.class);
    private List<EstoqueResponseDTO> ultimoEstoque;
    private List<ExpedicaoResponseDTO> ultimaExpedicao;

    // ------------------------------------------------------------------------------
    // Passada do write path (variáveis do CLP)
    // ------------------------------------------------------------------------------

    /**
     * Notificação do write path: a estação {@code estacao} acabou de ser lida e o bean
     * {@code dados} está atualizado. Publica heartbeat sempre; {@code estacao-all} e
     * {@code estacao-status} só se as variáveis mudaram desde a última emissão.
     */
    public synchronized void aoPassadaLida(EstacoesCLP estacao, EstacaoCLP dados) {
        publisher.publishEvent(new EstacaoHeartbeat(estacao.getFrontKey()));
        publicarAllSeMudou(estacao, dados);
        publicarStatusSeMudou(derivarStatus(estacao, dados));
    }

    private void publicarAllSeMudou(EstacoesCLP estacao, EstacaoCLP dados) {
        try {
            String json = mapper.writeValueAsString(dados);
            if (json.equals(ultimoJsonPorEstacao.get(estacao))) {
                return;
            }
            ultimoJsonPorEstacao.put(estacao, json);
            publisher.publishEvent(new EstacaoAllData(estacao.getFrontKey(), dados));
        } catch (Exception e) {
            log.error("[CLP {}] falha ao publicar estacao-all: {}", estacao.apiName(), e.getMessage());
        }
    }

    private void publicarStatusSeMudou(EstacaoStatusEvent atual) {
        EstacoesCLP estacao = EstacoesCLP.fromApi(atual.estacao());
        if (Objects.equals(atual, ultimoStatusPorEstacao.get(estacao))) {
            return;
        }
        ultimoStatusPorEstacao.put(estacao, atual);
        log.info("[CLP {}] status -> estado={} funcionamento={} (publicando no SSE)",
                estacao.apiName(), atual.estado(), atual.funcionamento());
        publisher.publishEvent(atual);
    }

    /** Deriva estado/funcionamento dos flags do bean (mesma lógica do antigo producer de status). */
    private EstacaoStatusEvent derivarStatus(EstacoesCLP estacao, EstacaoCLP dados) {
        boolean finish = dados.isFinishOP();
        boolean start = dados.isStartOP();
        boolean ocupado = dados.isOcupado();
        boolean aguardando = dados.isAguardando();
        boolean manual = dados.isManual();
        boolean emergencia = dados.isEmergencia();

        String estadoStr;
        if (emergencia) {
            estadoStr = "off";
        } else if (manual || ocupado) {
            estadoStr = "pause";
        } else {
            estadoStr = "on";
        }

        Integer funcionamento;
        if (finish || start) {
            funcionamento = 2;
        } else if (manual || ocupado) {
            funcionamento = 1;
        } else if (aguardando) {
            funcionamento = 0;
        } else {
            funcionamento = null;
        }

        return new EstacaoStatusEvent(estacao.getFrontKey(), estadoStr, funcionamento);
    }

    // ------------------------------------------------------------------------------
    // Mutações do banco (variáveis dos grids)
    // ------------------------------------------------------------------------------

    /** Estoque mutado (após commit): re-consulta e publica o grid só se o conteúdo mudou. */
    @TransactionalEventListener(fallbackExecution = true)
    public synchronized void onEstoqueMudou(EstoqueMudou ev) {
        try {
            List<EstoqueResponseDTO> atual = estoqueService.getTodos();
            if (!atual.equals(ultimoEstoque)) {
                ultimoEstoque = atual;
                publisher.publishEvent(new EstoqueGridEvent(atual));
            }
        } catch (Exception e) {
            log.error("Falha ao publicar grid de estoque: {}", e.getMessage());
        }
    }

    /** Expedição mutada (após commit): re-consulta e publica o grid só se o conteúdo mudou. */
    @TransactionalEventListener(fallbackExecution = true)
    public synchronized void onExpedicaoMudou(ExpedicaoMudou ev) {
        try {
            List<ExpedicaoResponseDTO> atual = expedicaoService.listarTodos();
            if (!atual.equals(ultimaExpedicao)) {
                ultimaExpedicao = atual;
                publisher.publishEvent(new ExpedicaoGridEvent(atual));
            }
        } catch (Exception e) {
            log.error("Falha ao publicar grid de expedição: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------------------
    // Watchdog offline
    // ------------------------------------------------------------------------------

    /**
     * Sem passada recente (IP não configurado, CLP fora, nenhum cliente SSE), as estações viram
     * {@code "off"} — on-change, então em regime estável não publica nada. Com nenhum cliente,
     * limpa os caches para reemitir tudo na reconexão.
     */
    @Scheduled(fixedDelayString = "${clp.watchdog.interval:1000}")
    public synchronized void watchdogOffline() {
        if (sseRegistry.count() == 0) {
            ultimoJsonPorEstacao.clear();
            ultimoStatusPorEstacao.clear();
            ultimoEstoque = null;
            ultimaExpedicao = null;
            return;
        }
        if (System.currentTimeMillis() - estado.getUltimoLeituraMillis() <= FRESCOR_MS) {
            return;
        }
        for (EstacoesCLP estacao : EstacoesCLP.values()) {
            publicarStatusSeMudou(new EstacaoStatusEvent(estacao.getFrontKey(), "off", null));
        }
    }
}
