package com.tecdes.smart.app_smart_40.service.sse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

/**
 * Canal de saída do SSE: registro dinâmico de clientes conectados + broadcast tolerante a falhas.
 *
 * <p>Mantém a lista de {@link SseEmitter} ativos ({@link CopyOnWriteArrayList}, segura para
 * iteração concorrente durante o broadcast). Conexões que entram/saem são tratadas dinamicamente;
 * uma conexão morta é removida no envio e <b>não</b> interrompe o envio aos demais clientes.
 */
@Component
@Slf4j
public class SseEmitterRegistry {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** Registra um emitter e agenda sua remoção quando a conexão termina, expira ou falha. */
    public SseEmitter add(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    /**
     * Envia {@code dado} (serializado em JSON) como evento nomeado {@code evento} a todos os clientes.
     * Cada envio é isolado em try/catch — um cliente morto é removido sem afetar os outros.
     */
    public void broadcast(String evento, Object dado) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(evento).data(dado, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    /** Quantidade de clientes atualmente conectados (útil para diagnóstico). */
    public int count() {
        return emitters.size();
    }
}
