package br.com.vadebicicleta.scb.aluguel;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class DevolucaoController {

	private final DevolucaoService devolucaoService;

	public DevolucaoController(DevolucaoService devolucaoService) {
		this.devolucaoService = devolucaoService;
	}

	@PostMapping("/api/devolucoes")
	public ResultadoDevolucao devolver(@Valid @RequestBody PedidoDevolucao pedido, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.devolucaoService.devolver(pedido);
	}

}
