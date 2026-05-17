package com.tecdes.smart.app_smart_40.util;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.tecdes.smart.app_smart_40.model.Estoque;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.enums.CorBloco;
import com.tecdes.smart.app_smart_40.repository.EstoqueRepository;
import com.tecdes.smart.app_smart_40.repository.ExpedicaoRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private EstoqueRepository estoqueRepository;
    @Autowired
    private ExpedicaoRepository expedicaoRepository;

    @Override
    public void run(String... args) throws Exception {

        if (estoqueRepository.count() == 0) {
            System.out.println(">> Estoque vazio. Inicializando...");
            estoqueRepository.saveAll(List.of(
                    Estoque.builder().posicao(1).corBloco(CorBloco.AZUL).build(),
                    Estoque.builder().posicao(2).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(3).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(4).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(5).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(6).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(7).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(8).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(9).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(10).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(11).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(12).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(13).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(14).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(15).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(16).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(17).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(18).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(19).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(20).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(21).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(22).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(23).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(24).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(25).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(26).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(27).corBloco(CorBloco.VAZIO).build(),
                    Estoque.builder().posicao(28).corBloco(CorBloco.VAZIO).build()));
            System.out.println(">> Estoque inicializado.");
        } else {
            System.out.println(">> Estoque já contém dados. Pulando.");
        }

        if (expedicaoRepository.count() == 0) {
            System.out.println(">> Expedicao vazia. Inicializando...");
            expedicaoRepository.saveAll(List.of(
                    Expedicao.builder().posicao(1).build(),
                    Expedicao.builder().posicao(2).build(),
                    Expedicao.builder().posicao(3).build(),
                    Expedicao.builder().posicao(4).build(),
                    Expedicao.builder().posicao(5).build(),
                    Expedicao.builder().posicao(6).build(),
                    Expedicao.builder().posicao(7).build(),
                    Expedicao.builder().posicao(8).build(),
                    Expedicao.builder().posicao(9).build(),
                    Expedicao.builder().posicao(10).build(),
                    Expedicao.builder().posicao(11).build(),
                    Expedicao.builder().posicao(12).build()));
            System.out.println(">> Expedicao inicializada.");
        } else {
            System.out.println(">> Expedicao já contém dados. Pulando.");
        }
    }
}