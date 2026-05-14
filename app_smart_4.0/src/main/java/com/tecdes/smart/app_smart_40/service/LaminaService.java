package com.tecdes.smart.app_smart_40.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tecdes.smart.app_smart_40.dto.LaminaDTO;
import com.tecdes.smart.app_smart_40.model.Lamina;
import com.tecdes.smart.app_smart_40.repository.LaminaRepository;

@Service
public class LaminaService {

    @Autowired
    private LaminaRepository laminaRepository;

    @Transactional
    public LaminaDTO salvar(LaminaDTO dto) {
        Lamina lamina = new Lamina();
        lamina.setCor(dto.cor());
        lamina.setPadrao(dto.padrao());
        lamina.setPosicaoNoBloco(dto.posicaoNoBloco());

        validarRegrasLamina(lamina);

        Lamina laminaSalva = laminaRepository.save(lamina);

        
        return new LaminaDTO(
            laminaSalva.getCor(),
            laminaSalva.getPadrao(),
            laminaSalva.getPosicaoNoBloco()
        );
    }

    public void validarRegrasLamina(Lamina lamina) {
        if (lamina.getCor() == null || lamina.getCor() < 1 || lamina.getCor() > 6) {
            throw new RuntimeException("Erro: Cor de lâmina inválida (1-6).");
        }
        if (lamina.getPosicaoNoBloco() == null || lamina.getPosicaoNoBloco() < 1 || lamina.getPosicaoNoBloco() > 3) {
            throw new RuntimeException("Erro: Posição da lâmina inválida.");
        }
        if (lamina.getPadrao() == null || lamina.getPadrao() < 0 || lamina.getPadrao() > 3) {
            throw new RuntimeException("Erro: Padrão da lâmina inválido (0-3).");
        }
    }
}