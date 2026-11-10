package br.com.vadebicicleta.scb.ciclista;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.vadebicicleta.scb.seguranca.AutenticacaoService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
public class CiclistaController {

	private final CiclistaService ciclistaService;

	private final AutenticacaoService autenticacaoService;

	public CiclistaController(CiclistaService ciclistaService, AutenticacaoService autenticacaoService) {
		this.ciclistaService = ciclistaService;
		this.autenticacaoService = autenticacaoService;
	}

	@PostMapping(value = "/api/ciclistas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CiclistaResposta> cadastrar(@Valid @ModelAttribute CadastroCiclistaForm form,
			@RequestParam(value = "foto", required = false) MultipartFile foto) {
		CiclistaResposta resposta = this.ciclistaService.cadastrar(form, foto);
		return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
	}

	@PostMapping("/api/login")
	public CiclistaResposta entrar(@Valid @RequestBody LoginForm form, HttpServletRequest request,
			HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.autenticacaoService.entrar(form, request, response);
	}

	/**
	 * Usa o e-mail da sessao. Um id enviado pelo navegador e ignorado.
	 */
	@GetMapping("/api/ciclista")
	public PerfilCiclista atual(Authentication autenticacao, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		return this.ciclistaService.consultarPerfil(autenticacao.getName());
	}

	@PutMapping(value = "/api/ciclista", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public AtualizacaoResposta atualizar(Authentication autenticacao, @Valid @ModelAttribute AtualizacaoCiclistaForm form,
			@RequestParam(value = "foto", required = false) MultipartFile foto, HttpServletRequest request,
			HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		AtualizacaoResposta resposta = this.ciclistaService.atualizar(autenticacao.getName(), form, foto);
		if (resposta.emailAlterado()) {
			encerrarSessao(request, response);
		}
		return resposta;
	}

	private static void encerrarSessao(HttpServletRequest request, HttpServletResponse response) {
		HttpSession sessao = request.getSession(false);
		if (sessao != null) {
			sessao.invalidate();
		}
		SecurityContextHolder.clearContext();
		Cookie cookie = new Cookie("JSESSIONID", "");
		cookie.setMaxAge(0);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		response.addCookie(cookie);
	}

}
