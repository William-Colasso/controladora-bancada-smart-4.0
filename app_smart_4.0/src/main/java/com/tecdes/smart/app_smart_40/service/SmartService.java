package com.tecdes.smart.app_smart_40.service;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Lamina;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartService {

    private final PedidoService pedidoService;

    private final PlcConnectionService plcConnectionService;

    private final PedidoRepository pedidoRepository;
    private final ExpedicaoService expedicaoService;
    private final EstoqueService estoqueService;
    private final ClpIpRegistry ipRegistry;
    private static final int TOTAL_SHORTS = 30;
    private static final int TOTAL_BYTES = TOTAL_SHORTS * 2;

    @Transactional
    public void enviarParaProducao(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido).orElseThrow();

        // É aqui — e só aqui — que o pedido "consome" recursos: reserva a posição
        // de expedição e dá baixa no estoque. criar() apenas valida disponibilidade
        // (checagem otimista), não reserva nada.
        reservarExpedicao(pedido);
        pedido.setStatus(StatusPedido.PRODUCAO);
        consumirEstoque(pedido);

        // Salva diretamente a entidade gerenciada pelo JPA (não via
        // pedidoService.atualizar(), que recriava a entidade sem pedido nos
        // blocos → PropertyValueException).
        pedidoRepository.save(pedido);

        enviarParaClp(pedido);
    }

    // Reserva a posição de expedição de forma lazy: a vinculação só acontece aqui,
    // no envio à produção. Se o pedido ainda está PENDENTE (expedicao == null),
    // pega a primeira posição livre agora.
    private void reservarExpedicao(Pedido pedido) {
        if (pedido.getExpedicao() == null) {
            Expedicao expedicao = expedicaoService.primeiraExpedicaoLivre().toEntity();
            pedido.setExpedicao(expedicao);
            expedicao.setPedido(pedido);
            expedicaoService.atualizarExpedicao(expedicao);
        }
    }

    // Dá baixa no estoque de cada bloco. A regra de negócio (encontrar posição,
    // blindar contra overselling, marcar VAZIO) vive no EstoqueService.
    private void consumirEstoque(Pedido pedido) {
        pedido.getBlocos().forEach(estoqueService::vincularEDarBaixa);
    }

    // Serializa o pedido e envia ao CLP. Falha de conexão/escrita é logada e não
    // derruba a transação de consumo já persistida acima.
    private void enviarParaClp(Pedido pedido) {
        byte[] buffer = converterParaBytes(pedido);
        printHex(buffer);

        // O payload do pedido (writeBlock DB9) e as flags de início vão para o CLP da estação
        // ESTOQUE; o IP vem do mesmo ClpIpRegistry usado pela leitura SSE (alterável em runtime).
        String ipClp = ipRegistry.getIp(EstacaoClp.ESTOQUE);
        PlcConnector connector = plcConnectionService.getConnection(ipClp);
        if (connector != null) {
            try {
                connector.writeBlock(9, 2, 60, buffer);
                log.info("Dados enviados para o CLP: {}", ipClp);
                enviarTampa(pedido.getCorTampa().getValue());
                iniciarExecucaoPedido(ipClp);
            } catch (Exception ex) {
                log.error("Erro ao enviar dados para o CLP: {}", ex.getMessage());
            }
        } else {
            log.warn("CLP não disponível: {}", ipClp);
        }
    }

    private void escreverBloco(Bloco bloco, ByteBuffer buffer) {
        buffer.putShort((short) bloco.getCor().getValue()); // Cor_Andar
        buffer.putShort((short) bloco.getEstoque().getPosicao().intValue()); // Posicao_Estoque_Andar

        List<Lamina> laminas = bloco.getLaminas();

        Lamina[] slots = new Lamina[3];
        for (Lamina lamina : laminas) {
            int pos = lamina.getPosicao().getValue();
            slots[pos - 1] = lamina;
        }

        for (Lamina slot : slots) {
            buffer.putShort(slot != null ? (short) slot.getCor().getValue() : (short) 0);
        }

        for (Lamina slot : slots) {
            buffer.putShort(slot != null ? (short) slot.getPadrao().getValue() : (short) 0);
        }

        buffer.putShort((short) 0);
    }

    private void escreverBlocoVazio(ByteBuffer buffer) {
        for (int i = 0; i < 9; i++) {
            buffer.putShort((short) 0);
        }
    }

    private byte[] converterParaBytes(Pedido pedido) {
        ByteBuffer buffer = ByteBuffer.allocate(60);

        List<Bloco> blocos = pedido.getBlocos();

        for (int i = 0; i < 3; i++) {
            if (i < blocos.size()) {
                escreverBloco(blocos.get(i), buffer);
            } else {
                escreverBlocoVazio(buffer);
            }
        }

        // Offset 56
        buffer.putShort((short) pedido.getOrdemProducao().intValue()); // Numero_Pedido
        buffer.putShort((short) pedido.getTipoPedido().getValue()); // Andares
        buffer.putShort((short) pedido.getExpedicao().getPosicao().intValue()); // Posicao_Expedicao

        return buffer.array();
    }

    // Printar bloco de bytes do Pedido no console
    public void printHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        System.out.println("--- BLOCO DE BYTES (HEXADECIMAL) ---");

        for (int i = 0; i < bytes.length; i++) {
            // Converte o byte para Hex e garante que tenha 2 dígitos (ex: 0A em vez de A)
            sb.append(String.format("%02X ", bytes[i]));

            // Opcional: Quebra de linha a cada 10 bytes para facilitar a leitura
            if ((i + 1) % 10 == 0) {
                sb.append("\n");
            }
        }

        System.out.println(sb.toString());
        System.out.println("------------------------------------");
    }

    // Envia comando para a Planta Smart iniciar a produção do Pedido
    public void iniciarExecucaoPedido(String ipClp) {
        PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);
        if (plcConnector == null) {
            return;
        }

        try {

            // Inicializa as flags da estação ESTOQUE
            // plcConnector.connect();
            plcConnector.writeBit(9, 0, 0, Boolean.parseBoolean("FALSE"));
            plcConnector.writeBit(9, 64, 0, Boolean.parseBoolean("FALSE"));
            plcConnector.writeBit(9, 64, 1, Boolean.parseBoolean("FALSE"));
            plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE"));

            // plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE"));
            // Iniciar pedido
            System.out.println("SETAR FLAG INICIAR PEDIDO");
            plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("TRUE"));

            Thread.sleep(800);

            System.out.println("RESETAR FLAG INICIAR PEDIDO");
            plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE"));

        } catch (Exception ex) {

        }
    }

    public void enviarTampa(int tampa) {
        System.out.println("\n\nSELETOR DE TAMPAS INSTALADO NA BANCADA\n\n");
        // Passo 2) Selecionar a tampa via POST
        try {
            RestTemplate apiSeletorTampa = new RestTemplate();
            String url = "http://10.74.241.245/api/move_pos";

            // 1. Definir o cabeçalho como application/x-www-form-urlencoded
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 2. Usar MultiValueMap (específico para formulários no Spring)
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("pos", String.valueOf(tampa));
            map.add("offset", "0");

            // 3. Criar a entidade com cabeçalhos e corpo
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            // 4. Tente ler a resposta primeiro como String para ver o que o ESP32 está
            // realmente enviando
            ResponseEntity<String> rawResponse = apiSeletorTampa.postForEntity(url, request, String.class);
            System.out.println("Resposta Bruta do ESP32: " + rawResponse.getBody());

            // 5. Agora, para a sua lógica de negócio, usamos o Map
            ResponseEntity<Map> response = apiSeletorTampa.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();

            // Verificação robusta
            if (body == null || body.get("status") == null) {
                System.out.println("Deu erro");
                return;
            }

            String status = body.get("status").toString();

            // Verificação flexível (ignora maiúsculas/minúsculas)
            if (!status.toLowerCase().contains("ok")) {
                System.out.println("Deu erro");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Deu erro");
            return;
        }
    }

}