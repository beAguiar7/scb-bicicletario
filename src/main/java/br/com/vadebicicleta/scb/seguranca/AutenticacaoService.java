package br.com.vadebicicleta.scb.seguranca;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import br.com.vadebicicleta.scb.ciclista.CiclistaResposta;
import br.com.vadebicicleta.scb.ciclista.CiclistaService;
import br.com.vadebicicleta.scb.ciclista.LoginForm;
import br.com.vadebicicleta.scb.erro.ErroDeNegocio;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AutenticacaoService {

	private final AuthenticationManager authenticationManager;

	private final CiclistaService ciclistaService;

	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	/**
	 * Troca o identificador da sessao depois do login. E o mecanismo do Spring Security
	 * contra fixacao de sessao.
	 */
	private final ChangeSessionIdAuthenticationStrategy renovacaoDeSessao = new ChangeSessionIdAuthenticationStrategy();

	public AutenticacaoService(AuthenticationManager authenticationManager, CiclistaService ciclistaService) {
		this.authenticationManager = authenticationManager;
		this.ciclistaService = ciclistaService;
	}

	public CiclistaResposta entrar(LoginForm form, HttpServletRequest request, HttpServletResponse response) {
		Authentication autenticacao;
		try {
			autenticacao = this.authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(form.getEmail(), form.getSenha()));
		}
		catch (org.springframework.security.core.AuthenticationException ex) {
			throw ErroDeNegocio.credenciaisInvalidas();
		}
		if (autenticacao instanceof CredentialsContainer container) {
			container.eraseCredentials();
		}

		SecurityContext contexto = SecurityContextHolder.createEmptyContext();
		contexto.setAuthentication(autenticacao);
		SecurityContextHolder.setContext(contexto);
		request.getSession(true);
		this.renovacaoDeSessao.onAuthentication(autenticacao, request, response);
		this.securityContextRepository.saveContext(contexto, request, response);
		return this.ciclistaService.consultarPorEmail(autenticacao.getName());
	}

}
