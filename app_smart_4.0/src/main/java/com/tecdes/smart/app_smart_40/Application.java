package com.tecdes.smart.app_smart_40;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

// Swagger UI: /swagger-ui.html · spec: /v3/api-docs
@OpenAPIDefinition(info = @Info(
		title = "Controladora Bancada SMART 4.0",
		version = "0.0.1",
		description = """
				API REST da bancada de manufatura SMART 4.0 (estoque → produção → expedição), \
				integrada a CLPs Siemens S7 (porta 102) e a um ESP32 (tampa).

				**Todos os enums trafegam como inteiros no JSON:**

				| Enum | Valores |
				|---|---|
				| `status` (pedido) | 1=PENDENTE · 2=PRODUCAO · 3=CONCLUIDO |
				| `tipoPedido` | 1=SIMPLES · 2=DUPLO · 3=TRIPLO (nº de blocos) |
				| `corTampa` | 1=PRETO · 2=VERMELHO · 3=AZUL |
				| `cor` (bloco) / `corBloco` | 0=VAZIO · 1=PRETO · 2=VERMELHO · 3=AZUL |
				| `andar` (bloco) | 1=PRIMEIRO · 2=SEGUNDO · 3=TERCEIRO |
				| `cor` (lâmina) | 1=VERMELHO · 2=AZUL · 3=AMARELO · 4=VERDE · 5=PRETO · 6=BRANCO |
				| `padrao` (lâmina) | 0=NENHUM · 1=CASA · 2=NAVIO · 3=ESTRELA |
				| `posicao` (lâmina) | 1=ESQUERDA · 2=FRENTE · 3=DIREITA |

				Erros seguem o formato `ErrorResponseDTO`: `{status, message, path, timestamp}` \
				(400 = requisição/estado inválido, 404 = não encontrado, 422 = estoque insuficiente).

				Estado em tempo real (grids, status das estações) chega por **SSE** em `GET /api/stream` \
				— eventos: `estacao-status`, `estacao-all`, `estacao-heartbeat`, `estoque`, `expedicao`."""))
@SpringBootApplication
@EnableScheduling // produtores read-only do SSE rodam em @Scheduled
@EnableAsync      // broadcast do SseNotifier roda fora da thread do @Scheduled
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
