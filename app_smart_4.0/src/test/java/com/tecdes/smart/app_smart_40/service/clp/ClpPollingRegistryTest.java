package com.tecdes.smart.app_smart_40.service.clp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;

@DisplayName("ClpPollingRegistry")
class ClpPollingRegistryTest {

    /** O registry não tem dependências — os intervalos-semente entram direto pelo construtor. */
    private ClpPollingRegistry registry(long estoque, long processo, long montagem, long expedicao) {
        return new ClpPollingRegistry(estoque, processo, montagem, expedicao);
    }

    @Test
    @DisplayName("boot - cada estação recebe o intervalo semeado (fallback já resolvido pelo Spring)")
    void boot_semeiaIntervalos() {
        ClpPollingRegistry registry = registry(300, 500, 250, 1000);

        assertThat(registry.getIntervalo(EstacoesCLP.ESTOQUE)).isEqualTo(300);
        assertThat(registry.getIntervalo(EstacoesCLP.PROCESSO)).isEqualTo(500);
        assertThat(registry.getIntervalo(EstacoesCLP.MONTAGEM)).isEqualTo(250);
        assertThat(registry.getIntervalo(EstacoesCLP.EXPEDICAO)).isEqualTo(1000);
    }

    @Test
    @DisplayName("setIntervalo - grava valor válido e reflete no getIntervalo/snapshot")
    void setIntervalo_valido_gravaEReflete() {
        ClpPollingRegistry registry = registry(300, 300, 300, 300);

        long gravado = registry.setIntervalo(EstacoesCLP.ESTOQUE, 750L);

        assertThat(gravado).isEqualTo(750);
        assertThat(registry.getIntervalo(EstacoesCLP.ESTOQUE)).isEqualTo(750);
        assertThat(registry.snapshot().get(EstacoesCLP.ESTOQUE)).isEqualTo(750);
    }

    @Test
    @DisplayName("setIntervalo - valor <= 0 lança 400 (IllegalArgument)")
    void setIntervalo_zeroOuNegativo_lanca() {
        ClpPollingRegistry registry = registry(300, 300, 300, 300);

        assertThatThrownBy(() -> registry.setIntervalo(EstacoesCLP.ESTOQUE, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.setIntervalo(EstacoesCLP.ESTOQUE, -5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setIntervalo - null lança 400 (IllegalArgument)")
    void setIntervalo_null_lanca() {
        ClpPollingRegistry registry = registry(300, 300, 300, 300);

        assertThatThrownBy(() -> registry.setIntervalo(EstacoesCLP.ESTOQUE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
