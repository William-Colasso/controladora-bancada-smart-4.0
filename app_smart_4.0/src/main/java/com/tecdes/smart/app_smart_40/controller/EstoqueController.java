package com.tecdes.smart.app_smart_40.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.service.EstoqueService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping("/disponivel")
    public List<Estoque> getDisponivel() {
        return estoqueService.getDisponivel();
    }
}