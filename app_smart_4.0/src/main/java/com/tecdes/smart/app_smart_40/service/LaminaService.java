package com.tecdes.smart.app_smart_40.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tecdes.smart.app_smart_40.dto.request.LaminaRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.LaminaResponseDTO;
import com.tecdes.smart.app_smart_40.model.Lamina;
import com.tecdes.smart.app_smart_40.repository.LaminaRepository;

@Service
public class LaminaService {

    @Autowired
    private LaminaRepository laminaRepository;

    @Transactional
    public LaminaResponseDTO salvar(LaminaRequestDTO dto) {
        Lamina lamina = new Lamina();
        lamina.setCor(dto.cor());
        lamina.setPadrao(dto.padrao());
        lamina.setPosicao(dto.posicao());

        validarRegrasLamina(lamina);

        Lamina laminaSalva = laminaRepository.save(lamina);

        return LaminaResponseDTO.fromEntity(laminaSalva);
    }

    public void validarRegrasLamina(Lamina lamina) {
        if (lamina.getCor() == null || lamina.getCor().getValue()< 1 || lamina.getCor().getValue() > 6) {
            throw new RuntimeException("Erro: Cor de lâmina inválida (1-6).");
        }
        if (lamina.getPosicao() == null || lamina.getPosicao().getValue() < 1 ||  lamina.getPosicao().getValue() > 3) {
            throw new RuntimeException("Erro: Posição da lâmina inválida.");
        }
        if (lamina.getPadrao().getValue() == -1 || lamina.getPadrao().getValue() < 0 || lamina.getPadrao().getValue() > 3) {
            throw new RuntimeException("Erro: Padrão da lâmina inválido (0-3).");
        }
    }
}