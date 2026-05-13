package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecdes.smart.app_smart_40.dto.BlocoDTO;
import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.Lamina;
import com.tecdes.smart.app_smart_40.repository.BlocoRepository;

@Service
public class BlocoService {

    @Autowired
    private BlocoRepository blocoRepository;

    @Autowired
    private LaminaService laminaService;

    @Transactional
    public Bloco salvarBloco(BlocoDTO dto, Integer tipoPedido) {
        validarRegrasDeOuro(dto, tipoPedido);

        Bloco bloco = new Bloco();
        bloco.setIdPedido(dto.idPedido());
        bloco.setIdEstoque(dto.idEstoque());
        bloco.setCor(dto.cor());

        if (dto.laminas() != null) {
            List<Lamina> entidadesLaminas = dto.laminas().stream().map(lDTO -> {
                Lamina lamina = new Lamina();
                lamina.setCor(lDTO.cor());
                lamina.setPadrao(lDTO.padrao());
                lamina.setPosicaoNoBloco(lDTO.posicaoNoBloco());
                
                laminaService.validarRegrasLamina(lamina);
                
                lamina.setBloco(bloco);
                
                return lamina;
            }).collect(Collectors.toList());

            bloco.setLaminas(entidadesLaminas);
        }

        return blocoRepository.save(bloco);
    }

    private void validarRegrasDeOuro(BlocoDTO dto, Integer tipoPedido) {
        if (dto.laminas() != null && dto.laminas().size() > 3) {
            throw new RuntimeException("Erro: O bloco excede o limite permitido de 3 lâminas.");
        }

        if (dto.cor() == null || dto.cor() < 1 || dto.cor() > 3) {
            throw new RuntimeException("Erro: Cor do bloco inválida para a produção.");
        }

        if (dto.idEstoque() == null || dto.idEstoque() <= 0) {
            throw new RuntimeException("Erro: Posição de estoque inválida ou não informada.");
        }
    }
}