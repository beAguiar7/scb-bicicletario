package br.com.vadebicicleta.scb.aluguel;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@RestController
public class TaxaExtraController {

	private final TaxaExtraService taxaExtraService;

	public TaxaExtraController(TaxaExtraService taxaExtraService) {
		this.taxaExtraService = taxaExtraService;
	}

	@GetMapping("/api/taxas-extras")
	public List<TaxaPendenteResposta> listar(Authentication autenticacao, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.taxaExtraService.listarPendentes(autenticacao.getName());
	}

	@PostMapping("/api/taxas-extras/{id}/pagamento")
	public ResultadoPagamentoTaxa pagar(Authentication autenticacao, @PathVariable Long id, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.taxaExtraService.pagar(autenticacao.getName(), id);
	}

}
