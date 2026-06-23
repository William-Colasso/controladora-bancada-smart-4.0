package com.tecdes.smart.app_smart_40.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpComandoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClpComandoController")
class ClpComandoControllerTest {

    @Mock
    private ClpComandoService comandoService;

    @InjectMocks
    private ClpComandoController controller;

    @Test
    @DisplayName("processar - estação válida → delega e responde 200")
    void processar_valida_delega() {
        ResponseEntity<Map<String, Object>> resp = controller.processar("processo");

        verify(comandoService).processar(EstacaoClp.PROCESSO);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("estacao", "processo").containsEntry("ok", true);
    }

    @Test
    @DisplayName("processar - estação inválida → IllegalArgument (400)")
    void processar_invalida_lanca() {
        assertThatThrownBy(() -> controller.processar("zzz"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("processarTodas - delega e responde 200")
    void processarTodas_delega() {
        ResponseEntity<Map<String, Object>> resp = controller.processarTodas();

        verify(comandoService).processarTodas();
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("ok", true);
    }
}
