package br.com.vadebicicleta.scb.seguranca;

import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O frontend pede o token antes do cadastro ou login e de novo na tela inicial.
 * O valor fica apenas na memoria da pagina, nao no localStorage.
 */
@RestController
public class CsrfController {

	@GetMapping("/api/csrf")
	public Map<String, String> token(CsrfToken csrfToken) {
		return Map.of(
				"headerName", csrfToken.getHeaderName(),
				"token", csrfToken.getToken());
	}

}
