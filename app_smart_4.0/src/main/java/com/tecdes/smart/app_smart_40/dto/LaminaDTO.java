package com.tecdes.smart.app_smart_40.dto;

import com.tecdes.smart.app_smart_40.model.enums.CorLamina;
import com.tecdes.smart.app_smart_40.model.enums.PadraoLamina;
import com.tecdes.smart.app_smart_40.model.enums.PosicaoLamina;

public record LaminaDTO(
    CorLamina cor,
    PadraoLamina padrao,
    PosicaoLamina posicaoNoBloco
) {}