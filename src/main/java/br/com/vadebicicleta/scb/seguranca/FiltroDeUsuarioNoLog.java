package br.com.vadebicicleta.scb.seguranca;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Coloca o e-mail da sessao no log. Usuario anonimo nao e tratado como identificado.
 */
class FiltroDeUsuarioNoLog extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		String usuario = "nao-identificado";
		if (autenticacao != null && autenticacao.isAuthenticated()
				&& !(autenticacao instanceof AnonymousAuthenticationToken)) {
			usuario = autenticacao.getName();
		}
		MDC.put("usuario", usuario);
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove("usuario");
		}
	}

}
