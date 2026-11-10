package br.com.vadebicicleta.scb.cartao;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class CartaoController {

	private final CartaoService cartaoService;

	public CartaoController(CartaoService cartaoService) {
		this.cartaoService = cartaoService;
	}

	@GetMapping("/api/cartoes-demonstracao")
	public List<CartaoOpcao> opcoes(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.cartaoService.listarOpcoes();
	}

	@GetMapping("/api/cartao")
	public CartaoResposta atual(Authentication autenticacao, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.cartaoService.consultar(autenticacao.getName());
	}

	@PostMapping("/api/cartao")
	public CartaoResposta salvar(Authentication autenticacao, @Valid @RequestBody PedidoCartao pedido,
			HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.cartaoService.salvar(autenticacao.getName(), pedido.referencia());
	}

}
