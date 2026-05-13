package com.tecdes.smart.app_smart_40.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.repository.BlocoRepository;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class BlocoService {

    @Autowired
    private BlocoRepository blocoRepository;

    @Autowired
    private LaminaService laminaService; 
    @Transactional
    public Bloco salvarBloco(Bloco bloco, Integer tipoPedido) {
        validarRegrasDeOuro(bloco, tipoPedido);

        if (bloco.getLaminas() != null) {
            bloco.getLaminas().forEach(lamina -> {
                laminaService.validarRegrasLamina(lamina); 
                lamina.setBloco(bloco);
            });
        }

        return blocoRepository.save(bloco);
    }

    private void validarRegrasDeOuro(Bloco bloco, Integer tipoPedido) {
        if (bloco.getLaminas() != null && bloco.getLaminas().size() > 3) {//•	Regra da Lâmina: Garantir que cada bloco tenha no máximo 3 lâminas
            throw new RuntimeException("Erro: O bloco excede o limite permitido de 3 lâminas.");
        }

        if (bloco.getCor() == null || bloco.getCor() < 1 || bloco.getCor() > 3) {
            throw new RuntimeException("Erro: Cor do bloco inválida para a produção.");
        }

        if (bloco.getIdEstoque() == null || bloco.getIdEstoque() <= 0) {
            throw new RuntimeException("Erro: Posição de estoque inválida ou não informada.");
        }
    }
}