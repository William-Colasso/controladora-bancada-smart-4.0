package com.tecdes.smart.app_smart_40.util;

import java.util.Comparator;
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
                    Estoque.builder().posicao(1).build(),
                    Estoque.builder().posicao(2).build(),
                    Estoque.builder().posicao(3).build(),
                    Estoque.builder().posicao(4).build(),
                    Estoque.builder().posicao(5).build(),
                    Estoque.builder().posicao(6).build(),
                    Estoque.builder().posicao(7).build(),
                    Estoque.builder().posicao(8).build(),
                    Estoque.builder().posicao(9).build(),
                    Estoque.builder().posicao(10).build(),
                    Estoque.builder().posicao(11).build(),
                    Estoque.builder().posicao(12).build(),
                    Estoque.builder().posicao(13).build(),
                    Estoque.builder().posicao(14).build(),
                    Estoque.builder().posicao(15).build(),
                    Estoque.builder().posicao(16).build(),
                    Estoque.builder().posicao(17).build(),
                    Estoque.builder().posicao(18).build(),
                    Estoque.builder().posicao(19).build(),
                    Estoque.builder().posicao(20).build(),
                    Estoque.builder().posicao(21).build(),
                    Estoque.builder().posicao(22).build(),
                    Estoque.builder().posicao(23).build(),
                    Estoque.builder().posicao(24).build(),
                    Estoque.builder().posicao(25).build(),
                    Estoque.builder().posicao(26).build(),
                    Estoque.builder().posicao(27).build(),
                    Estoque.builder().posicao(28).build()));

            System.out.println(">> Estoque inicializado.");
        }

        // Redistribui as cores em toda inicialização
        List<Estoque> estoques = estoqueRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Estoque::getPosicao))
                .toList();

        CorBloco[] cores = {
                CorBloco.VAZIO,
                CorBloco.PRETO,
                CorBloco.VERMELHO,
                CorBloco.AZUL
        };

        for (int i = 0; i < estoques.size(); i++) {
            estoques.get(i).setCorBloco(cores[i % cores.length]);
        }

        estoqueRepository.saveAll(estoques);

        System.out.println(">> Cores do estoque redistribuídas.");

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