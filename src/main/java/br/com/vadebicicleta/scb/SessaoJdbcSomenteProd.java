package br.com.vadebicicleta.scb;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * A sessao JDBC so entra no perfil prod. Local e testes continuam com a sessao do servidor.
 */
public class SessaoJdbcSomenteProd implements EnvironmentPostProcessor {

	static final String CONFIGURACAO = "org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		for (String perfil : environment.getActiveProfiles()) {
			if ("prod".equals(perfil)) {
				return;
			}
		}
		String atual = environment.getProperty("spring.autoconfigure.exclude");
		String exclusoes = CONFIGURACAO;
		if (atual != null && !atual.isBlank() && !atual.contains(CONFIGURACAO)) {
			exclusoes = atual + "," + CONFIGURACAO;
		}
		Map<String, Object> valores = new HashMap<>();
		valores.put("spring.autoconfigure.exclude", exclusoes);
		environment.getPropertySources().addFirst(new MapPropertySource("sessao-jdbc-somente-prod", valores));
	}

}
