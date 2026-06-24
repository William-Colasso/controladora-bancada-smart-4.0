package com.tecdes.smart.app_smart_40.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.tecdes.smart.app_smart_40.dto.request.ClpIpUpdateRequest;
import com.tecdes.smart.app_smart_40.dto.response.ClpConexaoResponseDTO;
import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.ClpLeituraRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.ClpHealthService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClpConexaoController")
class ClpConexaoControllerTest {

    @Mock
    private ClpIpRegistry ipRegistry;
    @Mock
    private ClpHealthService healthService;
    @Mock
    private ClpLeituraRegistry leituraRegistry;
    @InjectMocks
    private ClpComandoController controller;

    @Test
    @DisplayName("conectar - CLP alcançável → grava IP, habilita leitura, responde leitura:true")
    void conectar_alcancavel_habilita() {
        when(ipRegistry.setIp(EstacaoClp.ESTOQUE, "10.0.0.1")).thenReturn("10.0.0.1");
        when(healthService.alcancavel("10.0.0.1")).thenReturn(true);

        ResponseEntity<ClpConexaoResponseDTO> resp =
                controller.conectar("estoque", new ClpIpUpdateRequest("10.0.0.1"));

        verify(leituraRegistry).habilitar(EstacaoClp.ESTOQUE);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo(new ClpConexaoResponseDTO("estoque", "10.0.0.1", true, true));
    }

    @Test
    @DisplayName("conectar - CLP inalcançável → não habilita, responde leitura:false")
    void conectar_inalcancavel_naoHabilita() {
        when(ipRegistry.setIp(EstacaoClp.ESTOQUE, "10.0.0.9")).thenReturn("10.0.0.9");
        when(healthService.alcancavel("10.0.0.9")).thenReturn(false);

        ResponseEntity<ClpConexaoResponseDTO> resp =
                controller.conectar("estoque", new ClpIpUpdateRequest("10.0.0.9"));

        verify(leituraRegistry, never()).habilitar(EstacaoClp.ESTOQUE);
        verify(leituraRegistry).desabilitar(EstacaoClp.ESTOQUE);
        assertThat(resp.getBody()).isEqualTo(new ClpConexaoResponseDTO("estoque", "10.0.0.9", false, false));
    }

    @Test
    @DisplayName("conectar - estação inválida → IllegalArgument (400)")
    void conectar_invalida_lanca() {
        assertThatThrownBy(() -> controller.conectar("zzz", new ClpIpUpdateRequest("10.0.0.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("desconectar - desabilita a leitura e responde leitura:false")
    void desconectar_desabilita() {
        when(ipRegistry.getIp(EstacaoClp.PROCESSO)).thenReturn("10.0.0.2");

        ResponseEntity<ClpConexaoResponseDTO> resp = controller.desconectar("processo");

        verify(leituraRegistry).desabilitar(EstacaoClp.PROCESSO);
        assertThat(resp.getBody()).isEqualTo(new ClpConexaoResponseDTO("processo", "10.0.0.2", false, false));
    }
}
