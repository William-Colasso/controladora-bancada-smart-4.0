package com.tecdes.smart.app_smart_40.service.clp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClpIpRegistry")
class ClpIpRegistryTest {

    @Mock
    private PlcConnectionService plcConnectionService;

    private ClpIpRegistry registry(String estoque, String processo, String montagem, String expedicao) {
        return new ClpIpRegistry(plcConnectionService, estoque, processo, montagem, expedicao);
    }

    @Test
    @DisplayName("setIp - grava IP válido (com trim) e reflete no snapshot")
    void setIp_valido_gravaEReflete() {
        ClpIpRegistry registry = registry("1.1.1.1", "1.1.1.1", "1.1.1.1", "1.1.1.1");

        String gravado = registry.setIp(EstacaoClp.ESTOQUE, "  192.168.0.50 ");

        assertThat(gravado).isEqualTo("192.168.0.50");
        assertThat(registry.getIp(EstacaoClp.ESTOQUE)).isEqualTo("192.168.0.50");
    }

    @Test
    @DisplayName("setIp - formato inválido lança 400 (IllegalArgument)")
    void setIp_formatoInvalido_lanca() {
        ClpIpRegistry registry = registry("1.1.1.1", "1.1.1.1", "1.1.1.1", "1.1.1.1");

        assertThatThrownBy(() -> registry.setIp(EstacaoClp.ESTOQUE, "nao-eh-ip"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setIp - octeto acima de 255 lança 400")
    void setIp_octetoInvalido_lanca() {
        ClpIpRegistry registry = registry("1.1.1.1", "1.1.1.1", "1.1.1.1", "1.1.1.1");

        assertThatThrownBy(() -> registry.setIp(EstacaoClp.ESTOQUE, "10.0.0.999"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setIp - fecha a conexão do IP antigo quando ninguém mais o usa")
    void setIp_ipAntigoOrfao_desconecta() {
        ClpIpRegistry registry = registry("10.0.0.1", "10.0.0.2", "10.0.0.3", "10.0.0.4");

        registry.setIp(EstacaoClp.ESTOQUE, "10.0.0.9");

        verify(plcConnectionService).disconnect("10.0.0.1");
    }

    @Test
    @DisplayName("setIp - não fecha a conexão se outra estação ainda usa o IP antigo")
    void setIp_ipAntigoCompartilhado_naoDesconecta() {
        ClpIpRegistry registry = registry("10.0.0.1", "10.0.0.1", "10.0.0.1", "10.0.0.1");

        registry.setIp(EstacaoClp.ESTOQUE, "10.0.0.9");

        verify(plcConnectionService, never()).disconnect(anyString());
    }

    @Test
    @DisplayName("fromApi - resolve nomes válidos e rejeita inválidos")
    void fromApi_validaNomes() {
        assertThat(EstacaoClp.fromApi("processo")).isEqualTo(EstacaoClp.PROCESSO);
        assertThat(EstacaoClp.fromApi("EXPEDICAO")).isEqualTo(EstacaoClp.EXPEDICAO);
        assertThatThrownBy(() -> EstacaoClp.fromApi("inexistente"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
