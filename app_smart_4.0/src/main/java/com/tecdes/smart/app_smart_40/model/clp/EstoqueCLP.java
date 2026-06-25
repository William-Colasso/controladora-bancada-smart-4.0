package com.tecdes.smart.app_smart_40.model.clp;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component
@Getter
@Setter
@NoArgsConstructor
public class EstoqueCLP extends EstacaoCLP {
    private int cor_Andar_1;
    private int posicao_Estoque_Andar_1;
    private int cor_Lamina_1_Andar_1;
    private int cor_Lamina_2_Andar_1;
    private int cor_Lamina_3_Andar_1;
    private int padrao_Lamina_1_Andar_1;
    private int padrao_Lamina_2_Andar_1;
    private int padrao_Lamina_3_Andar_1;
    private int processamento_Andar_1;

    private int cor_Andar_2;
    private int posicao_Estoque_Andar_2;
    private int cor_Lamina_1_Andar_2;
    private int cor_Lamina_2_Andar_2;
    private int cor_Lamina_3_Andar_2;
    private int padrao_Lamina_1_Andar_2;
    private int padrao_Lamina_2_Andar_2;
    private int padrao_Lamina_3_Andar_2;
    private int processamento_Andar_2;

    private int cor_Andar_3;
    private int posicao_Estoque_Andar_3;
    private int cor_Lamina_1_Andar_3;
    private int cor_Lamina_2_Andar_3;
    private int cor_Lamina_3_Andar_3;
    private int padrao_Lamina_1_Andar_3;
    private int padrao_Lamina_2_Andar_3;
    private int padrao_Lamina_3_Andar_3;
    private int processamento_Andar_3;

    private int numeroPedidoEst;
    private int andares;
    private int posicaoExpedicaoEst;

    private boolean iniciarPedido;

    private boolean recebidoEstoque;
    private boolean iniciarGuardarEst;
    private int posicaoGuardarEst;

    private byte[] posicoesOcupadas;

    private boolean pedirPosicaoEst;
    private int posicaoEstoque;
    private boolean adicionarEstoque;
    private boolean removerEstoque;
    private boolean retornoEstoqueCheio;
    private int corGuardarEstoque;
    
}