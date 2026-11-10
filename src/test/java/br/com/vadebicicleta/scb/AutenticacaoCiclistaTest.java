package br.com.vadebicicleta.scb;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AutenticacaoCiclistaTest {

	private static final String SENHA = "segredo12";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void loginCorretoRenovaSessaoEConsultaSomenteOUsuarioAutenticado() throws Exception {
		String emailAna = email("ana");
		String emailBruno = email("bruno");
		cadastrar(emailAna, "Ana Souza");
		cadastrar(emailBruno, "Bruno Lima");

		MockHttpSession sessao = new MockHttpSession();
		String idAntes = sessao.getId();

		// O cookie da sessao anterior deixa o identificador pedido valido, como no navegador.
		this.mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
				.session(sessao)
				.with(csrf())
				.with(requisicao -> {
					requisicao.setRequestedSessionId(idAntes);
					requisicao.setRequestedSessionIdValid(true);
					return requisicao;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(emailAna.toUpperCase(), SENHA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana Souza"))
				.andExpect(jsonPath("$.email").value(emailAna))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist())
				.andExpect(content().string(not(containsString(SENHA))))
				.andExpect(content().string(not(containsString("$2"))));

		assertNotEquals(idAntes, sessao.getId());

		this.mockMvc.perform(MockMvcRequestBuilders.get("/api/ciclista").session(sessao).param("id", "999"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(emailAna))
				.andExpect(content().string(not(containsString("Bruno Lima"))))
				.andExpect(content().string(not(containsString(SENHA))));
	}

	@Test
	void loginIncorretoUsaAMesmaMensagem() throws Exception {
		String email = email("incorreto");
		cadastrar(email, "Ana Souza");

		this.mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(email, "senhaerrada")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
				.andExpect(jsonPath("$.mensagem").value("Login ou senha inválidos"))
				.andExpect(content().string(not(containsString("senhaerrada"))));

		this.mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(email("nao-existe"), SENHA)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("Login ou senha inválidos"));
	}

	@Test
	void loginComCampoVazioNaoUsaAMensagemDeCredencial() throws Exception {
		this.mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"\",\"senha\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
				.andExpect(content().string(not(containsString("Login ou senha inválidos"))));
	}

	@Test
	void consultaSemAutenticacaoRetorna401() throws Exception {
		this.mockMvc.perform(MockMvcRequestBuilders.get("/api/ciclista"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
				.andExpect(jsonPath("$.mensagem").value("Faça login para continuar."))
				.andExpect(content().string(not(containsString("Exception"))));
	}

	@Test
	void logoutInvalidaASessao() throws Exception {
		String email = email("sair");
		cadastrar(email, "Ana Souza");
		MockHttpSession sessao = new MockHttpSession();

		this.mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(email, SENHA)))
				.andExpect(status().isOk());

		this.mockMvc.perform(MockMvcRequestBuilders.post("/api/logout").session(sessao).with(csrf()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(MockMvcRequestBuilders.get("/api/ciclista").session(sessao))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginSemCsrfERecusado() throws Exception {
		this.mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(email("csrf"), SENHA)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"))
				.andExpect(content().string(not(containsString(SENHA))));
	}

	private void cadastrar(String email, String nome) throws Exception {
		this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/ciclistas")
				.file(new MockMultipartFile("foto", "documento.jpg", "image/jpeg", ImagensTeste.jpeg()))
				.param("nome", nome)
				.param("nacionalidade", "BRASILEIRO")
				.param("numeroDocumento", "12345678901")
				.param("email", email)
				.param("senha", SENHA)
				.param("confirmacaoSenha", SENHA)
				.with(csrf()))
				.andExpect(status().isCreated());
	}

	private static String email(String prefixo) {
		return prefixo + "-" + UUID.randomUUID() + "@exemplo.test";
	}

	private static String json(String email, String senha) {
		return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
	}

}
