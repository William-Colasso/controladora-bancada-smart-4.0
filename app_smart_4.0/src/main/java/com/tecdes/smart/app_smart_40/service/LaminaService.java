package com.tecdes.smart.app_smart_40.service;

import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tecdes.smart.app_smart_40.model.Lamina;
import com.tecdes.smart.app_smart_40.repository.LaminaRepository;


@Service
public class LaminaService {

    @Autowired
    private LaminaRepository laminaRepository;

    public void validarRegrasLamina(Lamina lamina) {
        if (lamina.getCor() == null || lamina.getCor() < 1 || lamina.getCor() > 6) {
            throw new RuntimeException("Erro: Cor de lâmina inválida (1-6).");
        }
        if (lamina.getPosicaoNoBloco() == null || lamina.getPosicaoNoBloco() < 1 || lamina.getPosicaoNoBloco() > 3) {
            throw new RuntimeException("Erro: Posição da lâmina inválida.");
        }
    }
    
    @Transactional
    public Lamina salvar(Lamina lamina) {
        validarRegrasLamina(lamina);
        return laminaRepository.save(lamina);
    }
}