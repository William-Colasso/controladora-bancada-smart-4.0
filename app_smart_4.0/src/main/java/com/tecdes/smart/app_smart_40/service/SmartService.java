package com.tecdes.smart.app_smart_40.service;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.tecdes.smart.app_smart_40.model.Bloco;
import com.tecdes.smart.app_smart_40.model.Expedicao;
import com.tecdes.smart.app_smart_40.model.Lamina;
import com.tecdes.smart.app_smart_40.model.Pedido;
import com.tecdes.smart.app_smart_40.model.enums.EstacoesCLP;
import com.tecdes.smart.app_smart_40.model.enums.StatusPedido;
import com.tecdes.smart.app_smart_40.repository.PedidoRepository;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.TampaConfigRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

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
    private final TampaConfigRegistry tampaConfig;
    private final PlatformTransactionManager txManager;
    // Um RestTemplate por instância reusa o pool de conexões HTTP (não recriar por chamada).
    // ponytail: campo direto; extrair @Bean só quando houver segundo consumidor.
    private final RestTemplate apiSeletorTampa = new RestTemplate();
    private static final int TOTAL_SHORTS = 30;
    private static final int TOTAL_BYTES = TOTAL_SHORTS * 2;

    // O que a comunicação com o CLP precisa, materializado enquanto a sessão JPA está aberta
    // (payload já serializado + valor da tampa), para não tocar em lazy fora da transação.
    private record PayloadProducao(byte[] buffer, int tampa) {}

    // Persistência (reserva + baixa + save) roda numa transação CURTA via TransactionTemplate; a
    // comunicação com o CLP (socket + Thread.sleep de 800ms) fica FORA dela, para não segurar a
    // conexão JDBC do pool durante o I/O de rede. TransactionTemplate (e não @Transactional no
    // método) porque este bean chama a si mesmo — self-invocation não passa pelo proxy do Spring.
    public void enviarParaProducao(Long idPedido) {
        PayloadProducao payload = new TransactionTemplate(txManager).execute(status -> {
            Pedido pedido = pedidoRepository.findById(idPedido).orElseThrow();

            // É aqui — e só aqui — que o pedido "consome" recursos: reserva a posição
            // de expedição e dá baixa no estoque. criar() apenas valida disponibilidade
            // (checagem otimista), não reserva nada.
            reservarExpedicao(pedido);
            pedido.setStatus(StatusPedido.PRODUCAO);
            pedido.setDataEntradaProducao(java.time.LocalDateTime.now());
            consumirEstoque(pedido);

            // Salva diretamente a entidade gerenciada pelo JPA (não via
            // pedidoService.atualizar(), que recriava a entidade sem pedido nos
            // blocos → PropertyValueException).
            pedidoRepository.save(pedido);

            // Serializa o payload com a sessão aberta (blocos/lâminas são lazy).
            return new PayloadProducao(converterParaBytes(pedido), pedido.getCorTampa().getValue());
        });

        enviarParaClp(payload);
    }

    // Reserva a posição de expedição de forma lazy: a vinculação só acontece aqui,
    // no envio à produção. Se o pedido ainda está PENDENTE (expedicao == null),
    // pega a primeira posição livre agora.
    private void reservarExpedicao(Pedido pedido) {
        if (pedido.getExpedicao() == null) {
            Expedicao expedicao = expedicaoService.primeiraExpedicaoLivre().toEntity();
            pedido.setExpedicao(expedicao);
            expedicao.setPedidoAtual(pedido);
            expedicaoService.atualizarExpedicao(expedicao);
        }
    }

    // Dá baixa no estoque de cada bloco. A regra de negócio (encontrar posição,
    // blindar contra overselling, marcar VAZIO) vive no EstoqueService.
    private void consumirEstoque(Pedido pedido) {
        pedido.getBlocos().forEach(estoqueService::vincularEDarBaixa);
    }

    // Envia ao CLP o payload já serializado. Roda FORA da transação (ver enviarParaProducao):
    // falha de conexão/escrita é logada e não afeta o consumo já persistido.
    private void enviarParaClp(PayloadProducao payload) {
        printHex(payload.buffer());

        // O payload do pedido (writeBlock DB9) e as flags de início vão para o CLP da estação
        // ESTOQUE; o IP vem do mesmo ClpIpRegistry usado pela leitura SSE (alterável em runtime).
        String ipClp = ipRegistry.getIp(EstacoesCLP.ESTOQUE);
        PlcConnector connector = plcConnectionService.getConnection(ipClp);
        if (connector != null) {
            try {
                connector.writeBlock(9, 2, 60, payload.buffer());
                log.info("Dados enviados para o CLP: {}", ipClp);
                enviarTampa(payload.tampa());
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
            plcConnector.writeBit(9, 0, 0, false);
            plcConnector.writeBit(9, 64, 0, false);
            plcConnector.writeBit(9, 64, 1, false);
            plcConnector.writeBit(9, 62, 0, false);

            // Iniciar pedido
            log.debug("SETAR FLAG INICIAR PEDIDO");
            plcConnector.writeBit(9, 62, 0, true);

            Thread.sleep(800);

            log.debug("RESETAR FLAG INICIAR PEDIDO");
            plcConnector.writeBit(9, 62, 0, false);

        } catch (Exception ex) {
            log.error("Erro ao iniciar execução no CLP {}: {}", ipClp, ex.getMessage());
        }
    }

    public void enviarTampa(int tampa) {
        // Controladora de tampa existe só em algumas bancadas: desabilitada na config → no-op
        // silencioso (não é erro; ver /configuracao).
        if (!tampaConfig.isHabilitada()) {
            log.debug("Seletor de tampa desabilitado na configuração — comando ignorado.");
            return;
        }
        // Passo 2) Selecionar a tampa via POST
        try {
            String url = "http://" + tampaConfig.getIp() + "/api/move_pos";

            // 1. Definir o cabeçalho como application/x-www-form-urlencoded
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 2. Usar MultiValueMap (específico para formulários no Spring)
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("pos", String.valueOf(tampa));
            map.add("offset", "0");

            // 3. Criar a entidade com cabeçalhos e corpo
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            // Um único POST — o debug que lia a resposta como String antes disso fazia o ESP32
            // executar o movimento duas vezes.
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