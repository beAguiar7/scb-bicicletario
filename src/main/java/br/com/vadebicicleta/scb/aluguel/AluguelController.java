package br.com.vadebicicleta.scb.aluguel;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class AluguelController {

	private final AluguelService aluguelService;

	public AluguelController(AluguelService aluguelService) {
		this.aluguelService = aluguelService;
	}

	@GetMapping("/api/aluguel/taxa")
	public TaxaResposta taxa(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.aluguelService.taxa();
	}

	@GetMapping("/api/totens")
	public List<TotemDisponivel> totens(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.aluguelService.listarTotens();
	}

	@PostMapping("/api/alugueis")
	public ResultadoAluguel confirmar(Authentication autenticacao, @Valid @RequestBody PedidoAluguel pedido,
			HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.aluguelService.confirmar(autenticacao.getName(), pedido);
	}

	@GetMapping("/api/aluguel")
	public AluguelAtual ativo(Authentication autenticacao, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.aluguelService.consultarAtivo(autenticacao.getName());
	}

	@GetMapping("/api/mensagens")
	public List<MensagemResposta> mensagens(Authentication autenticacao, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.aluguelService.listarMensagens(autenticacao.getName());
	}

}
