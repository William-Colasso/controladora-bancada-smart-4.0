package com.tecdes.smart.app_smart_40.service.sse.producer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoAllData;
import com.tecdes.smart.app_smart_40.model.clp.EstadoProducaoService;
import com.tecdes.smart.app_smart_40.model.clp.EstoqueCLP;
import com.tecdes.smart.app_smart_40.service.sse.producer.clp_data.EstoqueCLPProducer;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstacaoCLPProducer (estacao-all on-change)")
class EstacaoCLPProducerTest {

    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private EstadoProducaoService estado;

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("publica só quando o bean muda (leitura fresca)")
    void publicaOnChange() {
        EstoqueCLP bean = new EstoqueCLP();
        when(estado.getUltimoLeituraMillis()).thenReturn(System.currentTimeMillis());
        EstoqueCLPProducer p = new EstoqueCLPProducer(publisher, estado, bean, mapper);

        p.poll();             // 1ª vez: muda vs "nunca publicado" → publica
        p.poll();             // sem mudança → não publica
        bean.setNumeroOP(7);
        p.poll();             // mudou → publica

        verify(publisher, times(2)).publishEvent(any(EstacaoAllData.class));
    }

    @Test
    @DisplayName("leitura stale (comunicação parada) → não publica")
    void staleNaoPublica() {
        EstoqueCLP bean = new EstoqueCLP();
        bean.setOcupado(true);
        when(estado.getUltimoLeituraMillis()).thenReturn(0L); // muito antigo
        EstoqueCLPProducer p = new EstoqueCLPProducer(publisher, estado, bean, mapper);

        p.poll();

        verify(publisher, never()).publishEvent(any());
    }
}
