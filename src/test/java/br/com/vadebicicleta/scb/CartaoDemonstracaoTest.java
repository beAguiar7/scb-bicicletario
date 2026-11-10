package br.com.vadebicicleta.scb;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import br.com.vadebicicleta.scb.cartao.CartaoRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CartaoDemonstracaoTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CartaoRepository cartoes;

	@Test
	void cadastraETrocaOCartaoFicticio() throws Exception {
		String email = ApoioApi.email("cartao");
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", ApoioApi.documento());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);

		this.mockMvc.perform(get("/api/cartao").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cadastrado").value(false));

		this.mockMvc.perform(get("/api/cartoes-demonstracao").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].referencia").value("DEMO-APROVADO"))
				.andExpect(jsonPath("$[1].referencia").value("DEMO-RECUSADO"))
				.andExpect(content().string(not(containsString("cvv"))));

		salvar(sessao, "DEMO-APROVADO")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cadastrado").value(true))
				.andExpect(jsonPath("$.referencia").value("DEMO-APROVADO"))
				.andExpect(jsonPath("$.mascara").value("**** **** **** 1111"))
				.andExpect(jsonPath("$.cobrancaSimuladaAprovada").value(true))
				.andExpect(jsonPath("$.aviso", containsString("será aprovada")))
				.andExpect(jsonPath("$.aviso", containsString("Nenhuma cobrança real")));

		salvar(sessao, "DEMO-RECUSADO")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.referencia").value("DEMO-RECUSADO"))
				.andExpect(jsonPath("$.mascara").value("**** **** **** 0002"))
				.andExpect(jsonPath("$.cobrancaSimuladaAprovada").value(false))
				.andExpect(jsonPath("$.aviso", containsString("será recusada")));

		assertEquals(1, this.cartoes.findByCiclista_Email(email).stream().count());
		assertEquals("DEMO-RECUSADO", this.cartoes.findByCiclista_Email(email).orElseThrow().getReferencia());
	}

	@Test
	void trocaInvalidaMantemOCartaoAnterior() throws Exception {
		String email = ApoioApi.email("troca");
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", ApoioApi.documento());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		salvar(sessao, "DEMO-APROVADO").andExpect(status().isOk());

		salvar(sessao, "CARTAO-REAL")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("CARTAO_DESCONHECIDO"));

		this.mockMvc.perform(get("/api/cartao").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.referencia").value("DEMO-APROVADO"))
				.andExpect(jsonPath("$.cobrancaSimuladaAprovada").value(true));
		assertTrue(this.cartoes.findByCiclista_Email(email).orElseThrow().isCobrancaSimuladaAprovada());
	}

	@Test
	void cartaoSemSessaoOuSemCsrfERecusado() throws Exception {
		this.mockMvc.perform(get("/api/cartao")).andExpect(status().isUnauthorized());
		this.mockMvc.perform(post("/api/cartao")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"referencia\":\"DEMO-APROVADO\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
	}

	private org.springframework.test.web.servlet.ResultActions salvar(MockHttpSession sessao, String referencia)
			throws Exception {
		return this.mockMvc.perform(post("/api/cartao")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"referencia\":\"" + referencia + "\"}"));
	}

}
