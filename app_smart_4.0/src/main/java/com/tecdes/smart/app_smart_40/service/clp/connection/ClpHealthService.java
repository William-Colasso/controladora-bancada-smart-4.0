package com.tecdes.smart.app_smart_40.service.clp.connection;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Teste de saúde do CLP: confirma que o serviço S7 responde antes de iniciar a leitura.
 *
 * <p>Abre um socket TCP na porta <b>102</b> (link S7 real) com timeout curto. Mais significativo que
 * um ping ICMP — atesta que o CLP está servindo S7, não só que a máquina existe na rede. Não usa o
 * pool ({@link PlcConnectionService}): é um teste puro, sem poluir o cache de conexões.
 */
@Component
@Slf4j
public class ClpHealthService {

    /** Porta do protocolo S7 (ISO-on-TCP). */
    private static final int PORTA_S7 = 102;
    /** Timeout do connect, em ms. */
    private static final int TIMEOUT_MS = 1500;

    /** {@code true} se um socket TCP na porta 102 do {@code ip} conecta dentro do timeout. */
    public boolean alcancavel(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip.trim(), PORTA_S7), TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            log.debug("CLP {} inalcançável na porta {}: {}", ip, PORTA_S7, e.getMessage());
            return false;
        }
    }
}
