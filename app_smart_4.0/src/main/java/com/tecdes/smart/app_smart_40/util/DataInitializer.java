package com.tecdes.smart.app_smart_40.util;

import java.util.ArrayList;
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

        // 1. INICIALIZAÇÃO DO ESTOQUE
        if (estoqueRepository.count() == 0) {
            System.out.println(">> Estoque vazio. Inicializando...");

            List<Estoque> novosEstoques = new ArrayList<>();
            // Loop de 1 a 28 para criar as posições
            for (int i = 1; i <= 28; i++) {
                novosEstoques.add(Estoque.builder()
                        .posicao(i)
                        .corBloco(CorBloco.VAZIO) // Valor padrão para evitar o erro null
                        .build());
            }
            
            estoqueRepository.saveAll(novosEstoques);
            System.out.println(">> Estoque inicializado.");
        }

        // 2. REDISTRIBUIÇÃO DAS CORES (Executa sempre)
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


        // 3. INICIALIZAÇÃO DA EXPEDIÇÃO
        if (expedicaoRepository.count() == 0) {
            System.out.println(">> Expedicao vazia. Inicializando...");

            List<Expedicao> novasExpedicoes = new ArrayList<>();
            // Loop de 1 a 12 para criar as expedições
            for (int i = 1; i <= 12; i++) {
                novasExpedicoes.add(Expedicao.builder()
                        .posicao(i)
                        // .outraPropriedade("valorPadrao") -> Se Expedicao tiver campo obrigatório, coloque aqui!
                        .build());
            }

            expedicaoRepository.saveAll(novasExpedicoes);
            System.out.println(">> Expedicao inicializada.");
        } else {
            System.out.println(">> Expedicao já contém dados. Pulando.");
        }
    }
}