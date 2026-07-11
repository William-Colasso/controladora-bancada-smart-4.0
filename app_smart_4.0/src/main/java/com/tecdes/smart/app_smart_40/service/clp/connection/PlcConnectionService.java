package com.tecdes.smart.app_smart_40.service.clp.connection;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PlcConnectionService {
    private final Map<String, PlcConnector> conexoes = new ConcurrentHashMap<>();

    /** Timeout (ms) do handshake TCP ao abrir a conexão S7. */
    @Value("${clp.socket.connect-timeout:800}")
    private int connectTimeoutMs;

    /** Timeout (ms) de cada leitura no socket S7 (SO_TIMEOUT). */
    @Value("${clp.socket.read-timeout:1500}")
    private int readTimeoutMs;

    /** Timeout (ms) do ping (probe TCP na porta 102) que testa o alcance da estação. */
    @Value("${clp.ping.timeout:500}")
    private int pingTimeoutMs;

    /**
     * Testa o alcance do CLP: probe TCP descartável na porta 102 (a porta do protocolo S7), com
     * timeout curto. É o "ping antes de comunicar" — não toca o pool de conexões. Retorna
     * {@code true} se a porta aceita a conexão dentro do timeout.
     *
     * <p>Usa TCP (não {@code InetAddress.isReachable}, que é ICMP: costuma exigir privilégio e não
     * testa a porta 102).
     */
    public boolean online(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, 102), pingTimeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized PlcConnector getConnection(String ip) {
        // lógica de obter/abrir conexão
        PlcConnector connector = conexoes.get(ip);
            if (connector == null) {
                System.out.println("=============================");
                System.out.println("NOVA CONEXÃO COM O CLP: " + ip);
                System.out.println("=============================");
                connector = new PlcConnector(ip, 102, connectTimeoutMs, readTimeoutMs);
                try {
                    connector.connect();
                    conexoes.put(ip, connector);
                } catch (Exception e) {
                    System.err.println("Erro ao conectar ao CLP " + ip);
                    e.printStackTrace();
                    return null;
                }
            }
            return connector;
    }

    public synchronized void disconnect(String ip) { 
        PlcConnector connector = conexoes.get(ip);
            if (connector != null) {
                try {
                    connector.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                conexoes.remove(ip);
            } 
    }

    public synchronized void closeAll() {
        System.out.println("=============================");
            System.out.println("ENCERRAR CONEXÕES COM OS CLPs");
            System.out.println("=============================");
            for (Map.Entry<String, PlcConnector> entry : conexoes.entrySet()) {
                String ip = entry.getKey();
                PlcConnector connector = entry.getValue();
                try {
                    if (connector != null) {
                        connector.disconnect();
                        //System.out.println("Conexão com " + ip + " encerrada com sucesso.");
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao encerrar conexão com " + ip + ": " + e.getMessage());
                }
            }
            conexoes.clear();
    }

}
