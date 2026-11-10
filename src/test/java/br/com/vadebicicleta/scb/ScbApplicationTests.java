package br.com.vadebicicleta.scb;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScbApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void testesUsamBancoEmMemoria() throws Exception {
		try (Connection conexao = this.dataSource.getConnection()) {
			String url = conexao.getMetaData().getURL();
			assertTrue(url.startsWith("jdbc:h2:mem:"), url);
			assertFalse(url.contains("dados/scb"), url);
		}
	}

	@Test
	void statusDeveIndicarQueOScbEstaFuncionando() throws Exception {
		this.mockMvc.perform(get("/api/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sistema").value("SCB"))
				.andExpect(jsonPath("$.mensagem", containsString("funcionando")));
	}

	@Test
	void paginasPublicasContinuamAcessiveis() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(status().isOk());
		this.mockMvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Entrar")));
		this.mockMvc.perform(get("/cadastro.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Criar cadastro")));
		this.mockMvc.perform(get("/inicio.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Sair")));
	}

	@Test
	void consoleDoH2NaoFicaPublico() throws Exception {
		this.mockMvc.perform(get("/h2-console")).andExpect(status().isUnauthorized());
	}

}
