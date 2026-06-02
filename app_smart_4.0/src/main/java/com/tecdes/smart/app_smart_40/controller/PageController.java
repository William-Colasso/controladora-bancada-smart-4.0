package com.tecdes.smart.app_smart_40.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.tecdes.smart.app_smart_40.dto.request.EstoqueRequestDTO;
import com.tecdes.smart.app_smart_40.dto.response.BlocoResponseDTO;
import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.service.BlocoService;
import com.tecdes.smart.app_smart_40.service.EstoqueService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final BlocoService blocoService;
    private final EstoqueService estoqueService;

    @GetMapping("/formulario")
    public String formulario(Model model) {

        List<CorBloco> cores = estoqueService.getDisponivel().stream().map(estoque -> estoque.corBloco()).toList();

        Integer qtdAzul = cores.stream().filter(cor -> cor.equals(CorBloco.AZUL)).toArray().length;
        Integer qtdVermelho = cores.stream().filter(cor -> cor.equals(CorBloco.VERMELHO)).toArray().length;
        Integer qtdPreto = cores.stream().filter(cor -> cor.equals(CorBloco.PRETO)).toArray().length;


        model.addAttribute("qtdAzul", qtdAzul);
        model.addAttribute("qtdVermelho", qtdVermelho);
        model.addAttribute("qtdPreto", qtdPreto);
        model.addAttribute("cores", cores);

        return "formulario";

    }

}
