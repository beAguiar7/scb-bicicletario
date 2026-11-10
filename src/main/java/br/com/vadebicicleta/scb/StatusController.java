package br.com.vadebicicleta.scb;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta publica para confirmar que o backend subiu.
 * Cartao e aluguel ficam para a proxima etapa.
 */
@RestController
public class StatusController {

	@GetMapping("/api/status")
	public StatusResposta status() {
		return new StatusResposta(
				"SCB",
				"O Sistema de Controle de Bicicletário está funcionando.");
	}

	public record StatusResposta(String sistema, String mensagem) {
	}

}
