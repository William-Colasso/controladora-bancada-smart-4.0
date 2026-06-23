package com.tecdes.smart.app_smart_40.service.sse.producer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.tecdes.smart.app_smart_40.dto.event.EstacaoStatusEvent;
import com.tecdes.smart.app_smart_40.model.enums.EstacaoClp;
import com.tecdes.smart.app_smart_40.service.clp.ClpIpRegistry;
import com.tecdes.smart.app_smart_40.service.clp.ClpLeituraRegistry;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnectionService;
import com.tecdes.smart.app_smart_40.service.clp.connection.PlcConnector;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstacaoStatusProducer (read-only)")
class EstacaoStatusProducerTest {

    private static final String IP = "10.0.0.99";

    @Mock
    private PlcConnectionService plcConnectionService;
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private ClpIpRegistry ipRegistry;
    @Mock
    private ClpLeituraRegistry leituraRegistry;
    @Mock
    private PlcConnector connector;

    // Produtor da estação PROCESSO: DB2, size 9, opByte 4 (start=0x04/finish=0x02), flagsByte 6.
    // Leitura habilitada por padrão (foco dos casos é a derivação do status).
    private ProcessoStatusProducer processoProducer() {
        when(leituraRegistry.isHabilitada(EstacaoClp.PROCESSO)).thenReturn(true);
        return new ProcessoStatusProducer(plcConnectionService, publisher, ipRegistry, leituraRegistry);
    }

    /** Bloco DB2 (9 bytes) com os bits de status posicionados em b[4] (OP) e b[6] (flags). */
    private byte[] blocoProcesso(int opByte, int flagsByte) {
        byte[] b = new byte[9];
        b[4] = (byte) opByte;
        b[6] = (byte) flagsByte;
        return b;
    }

    @Test
    @DisplayName("ocupado → estado on, funcionamento 0")
    void ocupado_estadoOnFuncZero() throws Exception {
        when(ipRegistry.getIp(EstacaoClp.PROCESSO)).thenReturn(IP);
        when(plcConnectionService.getConnection(IP)).thenReturn(connector);
        when(connector.readBlock(2, 0, 9)).thenReturn(blocoProcesso(0x00, 0x01)); // ocupado

        processoProducer().poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("producao", "on", 0));
    }

    @Test
    @DisplayName("startOP + ocupado → estado on, funcionamento 1")
    void start_funcionamentoUm() throws Exception {
        when(ipRegistry.getIp(EstacaoClp.PROCESSO)).thenReturn(IP);
        when(plcConnectionService.getConnection(IP)).thenReturn(connector);
        when(connector.readBlock(2, 0, 9)).thenReturn(blocoProcesso(0x04, 0x01)); // start + ocupado

        processoProducer().poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("producao", "on", 1));
    }

    @Test
    @DisplayName("finishOP → funcionamento 2")
    void finish_funcionamentoDois() throws Exception {
        when(ipRegistry.getIp(EstacaoClp.PROCESSO)).thenReturn(IP);
        when(plcConnectionService.getConnection(IP)).thenReturn(connector);
        when(connector.readBlock(2, 0, 9)).thenReturn(blocoProcesso(0x02, 0x01)); // finish + ocupado

        processoProducer().poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("producao", "on", 2));
    }

    @Test
    @DisplayName("aguardando → estado pause, funcionamento null")
    void aguardando_estadoPause() throws Exception {
        when(ipRegistry.getIp(EstacaoClp.PROCESSO)).thenReturn(IP);
        when(plcConnectionService.getConnection(IP)).thenReturn(connector);
        when(connector.readBlock(2, 0, 9)).thenReturn(blocoProcesso(0x00, 0x02)); // aguardando

        processoProducer().poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("producao", "pause", null));
    }

    @Test
    @DisplayName("emergencia → estado off")
    void emergencia_estadoOff() throws Exception {
        when(ipRegistry.getIp(EstacaoClp.PROCESSO)).thenReturn(IP);
        when(plcConnectionService.getConnection(IP)).thenReturn(connector);
        when(connector.readBlock(2, 0, 9)).thenReturn(blocoProcesso(0x00, 0x08)); // emergencia

        processoProducer().poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("producao", "off", null));
    }

    @Test
    @DisplayName("IP não configurado → estado off, sem tentar conectar")
    void ipNaoConfigurado_estadoOff() {
        when(ipRegistry.getIp(EstacaoClp.PROCESSO)).thenReturn(null);

        processoProducer().poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("producao", "off", null));
        verify(plcConnectionService, never()).getConnection(anyString());
    }

    @Test
    @DisplayName("conexão nula → estado off, sem ler bloco")
    void conexaoNula_estadoOff() throws Exception {
        when(ipRegistry.getIp(EstacaoClp.PROCESSO)).thenReturn(IP);
        when(plcConnectionService.getConnection(IP)).thenReturn(null);

        processoProducer().poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("producao", "off", null));
        verify(connector, never()).readBlock(anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("publica só quando o snapshot muda")
    void publicaSomenteEmMudanca() throws Exception {
        when(ipRegistry.getIp(EstacaoClp.PROCESSO)).thenReturn(IP);
        when(plcConnectionService.getConnection(IP)).thenReturn(connector);
        when(connector.readBlock(2, 0, 9)).thenReturn(blocoProcesso(0x00, 0x01)); // mesmo estado nas 2 leituras

        ProcessoStatusProducer producer = processoProducer();
        producer.poll();
        producer.poll();

        verify(publisher, times(1)).publishEvent(any(EstacaoStatusEvent.class));
    }

    @Test
    @DisplayName("read-only: nunca escreve no PLC")
    void readOnly_nuncaEscreve() throws Exception {
        when(ipRegistry.getIp(EstacaoClp.PROCESSO)).thenReturn(IP);
        when(plcConnectionService.getConnection(IP)).thenReturn(connector);
        when(connector.readBlock(2, 0, 9)).thenReturn(blocoProcesso(0x04, 0x01));

        processoProducer().poll();

        verify(connector, never()).writeBit(anyInt(), anyInt(), anyInt(), anyBoolean());
        verify(connector, never()).writeByte(anyInt(), anyInt(), anyByte());
        verify(connector, never()).writeInt(anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Estoque usa DB9 com offsets próprios (opByte 98, flagsByte 100)")
    void estoque_offsetsProprios() throws Exception {
        byte[] b = new byte[111];
        b[100] = 0x01; // ocupado
        when(leituraRegistry.isHabilitada(EstacaoClp.ESTOQUE)).thenReturn(true);
        when(ipRegistry.getIp(EstacaoClp.ESTOQUE)).thenReturn(IP);
        when(plcConnectionService.getConnection(IP)).thenReturn(connector);
        when(connector.readBlock(9, 0, 111)).thenReturn(b);

        new EstoqueStatusProducer(plcConnectionService, publisher, ipRegistry, leituraRegistry).poll();

        verify(publisher).publishEvent(new EstacaoStatusEvent("estoque", "on", 0));
    }

    @Test
    @DisplayName("leitura desabilitada → não lê o socket nem publica")
    void leituraDesabilitada_naoLeNemPublica() throws Exception {
        when(leituraRegistry.isHabilitada(EstacaoClp.PROCESSO)).thenReturn(false);

        new ProcessoStatusProducer(plcConnectionService, publisher, ipRegistry, leituraRegistry).poll();

        verify(ipRegistry, never()).getIp(any());
        verify(plcConnectionService, never()).getConnection(anyString());
        verify(connector, never()).readBlock(anyInt(), anyInt(), anyInt());
        verify(publisher, never()).publishEvent(any());
    }
}
