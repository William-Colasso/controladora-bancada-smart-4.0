package com.tecdes.smart.app_smart_40.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecdes.smart.app_smart_40.dto.BlocoDTO;
import com.tecdes.smart.app_smart_40.dto.EstoqueDTO;
import com.tecdes.smart.app_smart_40.dto.PedidoDTO;
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
    public BlocoDTO salvarBloco(BlocoDTO dto, Integer tipoPedido) {
        validarRegrasDeOuro(dto, tipoPedido);

        Bloco bloco = new Bloco();
        bloco.setPedido(dto.pedidoDTO().toEntity());
        bloco.setEstoque(EstoqueDTO.toEntity(dto.estoque()));
        bloco.setCor(dto.cor());

        if (dto.laminas() != null) {
            List<Lamina> entidadesLaminas = dto.laminas().stream().map(lDTO -> {
                Lamina lamina = new Lamina();
                lamina.setCor(lDTO.cor());
                lamina.setPadrao(lDTO.padrao());
                lamina.setPosicaoNoBloco(lDTO.posicaoNoBloco());
                lamina.setBloco(bloco);
                laminaService.validarRegrasLamina(lamina);
                return lamina;
            }).collect(Collectors.toList());
            bloco.setLaminas(entidadesLaminas);
        }

        Bloco blocoSalvo = blocoRepository.save(bloco);

        return new BlocoDTO(
                PedidoDTO.fromEntity(blocoSalvo.getPedido()),
                EstoqueDTO.fromEntity(blocoSalvo.getEstoque()),
                blocoSalvo.getCor(),
                dto.laminas());
    }

    private void validarRegrasDeOuro(BlocoDTO dto, Integer tipoPedido) {
        if (dto.laminas() != null && dto.laminas().size() > 3) {
            throw new RuntimeException("Erro: O bloco excede o limite permitido de 3 lâminas.");
        }

        if (dto.cor() == null || dto.cor().getValue() < 0 || dto.cor().getValue() > 3) {
            throw new RuntimeException("Erro: Cor do bloco inválida para a produção.");
        }

        if (dto.estoque() == null || dto.estoque().id() <= 0) {
            throw new RuntimeException("Erro: Posição de estoque inválida ou não informada.");
        }
    }
}