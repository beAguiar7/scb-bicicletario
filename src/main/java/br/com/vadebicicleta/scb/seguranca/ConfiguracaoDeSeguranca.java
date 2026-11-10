package br.com.vadebicicleta.scb.seguranca;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import tools.jackson.databind.ObjectMapper;

import br.com.vadebicicleta.scb.erro.ErroResposta;
import br.com.vadebicicleta.scb.erro.RegistroDeFalhas;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class ConfiguracaoDeSeguranca {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao) throws Exception {
		return configuracao.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain filtro(HttpSecurity http, ObjectMapper json, RegistroDeFalhas registro) throws Exception {
		// O handler simples devolve o mesmo token que o frontend envia no cabecalho.
		CsrfTokenRequestAttributeHandler csrf = new CsrfTokenRequestAttributeHandler();
		http
				.csrf(configuracao -> configuracao.csrfTokenRequestHandler(csrf))
				.sessionManagement(sessao -> sessao
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.sessionFixation().changeSessionId())
				.authorizeHttpRequests(rotas -> rotas
						.requestMatchers(HttpMethod.GET, "/", "/*.html", "/*.css", "/*.js", "/favicon.ico").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/status", "/api/csrf").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/ciclistas", "/api/login", "/api/logout").permitAll()
						.requestMatchers("/h2-console", "/h2-console/**").denyAll()
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(excecoes -> excecoes
						.authenticationEntryPoint((request, response, erro) -> {
							registro.registrar("NAO_AUTENTICADO", local(request), "Requisicao sem autenticacao", null);
							escrever(response, json, 401, "NAO_AUTENTICADO", "Faça login para continuar.");
						})
						.accessDeniedHandler((request, response, erro) -> tratarAcessoNegado(response, json, registro, request, erro)))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(logout -> logout
						.logoutUrl("/api/logout")
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("JSESSIONID")
						.logoutSuccessHandler((request, response, autenticacao) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
				.addFilterAfter(new FiltroDeUsuarioNoLog(), SecurityContextHolderFilter.class);
		return http.build();
	}

	private static void tratarAcessoNegado(HttpServletResponse response, ObjectMapper json, RegistroDeFalhas registro,
			jakarta.servlet.http.HttpServletRequest request, AccessDeniedException erro) throws IOException {
		if (erro instanceof CsrfException) {
			registro.registrar("CSRF_INVALIDO", local(request), "Token CSRF ausente ou invalido", null);
			escrever(response, json, 403, "CSRF_INVALIDO",
					"Não foi possível validar a requisição. Atualize a página e tente novamente.");
			return;
		}
		registro.registrar("ACESSO_NEGADO", local(request), "Acesso negado", null);
		escrever(response, json, 403, "ACESSO_NEGADO", "Acesso negado.");
	}

	private static void escrever(HttpServletResponse response, ObjectMapper json, int status, String codigo,
			String mensagem) throws IOException {
		response.setStatus(status);
		response.setCharacterEncoding("UTF-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader("Cache-Control", "no-store");
		json.writeValue(response.getWriter(), new ErroResposta(codigo, mensagem, java.util.List.of()));
	}

	private static String local(jakarta.servlet.http.HttpServletRequest request) {
		return request.getMethod() + " " + request.getRequestURI();
	}

}
