package com.tecdes.smart.app_smart_40.service.clp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;

@DisplayName("ClpLeituraRegistry")
class ClpLeituraRegistryTest {

    @Test
    @DisplayName("default - todas as estações iniciam desabilitadas")
    void default_todasDesabilitadas() {
        ClpLeituraRegistry registry = new ClpLeituraRegistry();

        for (EstacaoClp e : EstacaoClp.values()) {
            assertThat(registry.isHabilitada(e)).as(e.name()).isFalse();
        }
    }

    @Test
    @DisplayName("habilitar/desabilitar - alterna a flag de uma estação isoladamente")
    void habilitarDesabilitar_alterna() {
        ClpLeituraRegistry registry = new ClpLeituraRegistry();

        registry.habilitar(EstacaoClp.ESTOQUE);
        assertThat(registry.isHabilitada(EstacaoClp.ESTOQUE)).isTrue();
        assertThat(registry.isHabilitada(EstacaoClp.PROCESSO)).isFalse();

        registry.desabilitar(EstacaoClp.ESTOQUE);
        assertThat(registry.isHabilitada(EstacaoClp.ESTOQUE)).isFalse();
    }

    @Test
    @DisplayName("snapshot - reflete o estado de todas as estações")
    void snapshot_refleteTodas() {
        ClpLeituraRegistry registry = new ClpLeituraRegistry();
        registry.habilitar(EstacaoClp.MONTAGEM);

        assertThat(registry.snapshot())
                .containsEntry(EstacaoClp.MONTAGEM, true)
                .containsEntry(EstacaoClp.ESTOQUE, false)
                .hasSize(EstacaoClp.values().length);
    }
}
